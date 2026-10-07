package com.takej.mall.order.service;


import com.takej.mall.order.client.ProductClient;
import com.takej.mall.order.dto.CreateOrderRequest;
import com.takej.mall.order.dto.OrderItemRequest;
import com.takej.mall.order.dto.ProductSnapshot;
import com.takej.mall.order.entity.OrderInfo;
import com.takej.mall.order.entity.OrderItem;
import com.takej.mall.order.mapper.OrderInfoMapper;
import com.takej.mall.order.mapper.OrderItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import com.takej.mall.common.BusinessException;
import com.takej.mall.common.ErrorCode;
import com.takej.mall.order.dto.OrderDetailResponse;
import com.takej.mall.order.enums.OrderStatus;
import com.takej.mall.order.client.CartClient;
import com.takej.mall.order.dto.CartItemSnapshot;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;



/**
 * 订单核心业务：创建订单并保存订单明细。
 */
@Service
public class OrderService {
    /**
     * 订单服务日志对象。
     */
    private static final Logger log =
            LoggerFactory.getLogger(OrderService.class);
    /** 下单幂等标记在 Redis 中的 key 前缀。 */
    private static final String IDEMPOTENT_KEY_PREFIX =
            "takej:order:idempotent:";

    /** 下单幂等标记保留时间：足够覆盖用户重复点击的时间窗口。 */
    private static final Duration IDEMPOTENT_TTL =
            Duration.ofMinutes(10);

    private final ProductClient productClient;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartClient cartClient;
    /** 用于保存下单幂等标记，防止重复提交创建多笔订单。 */
    private final StringRedisTemplate redisTemplate;

    public OrderService(ProductClient productClient,
                        CartClient cartClient,
                        OrderInfoMapper orderInfoMapper,
                        OrderItemMapper orderItemMapper,
                        StringRedisTemplate redisTemplate) {
        this.productClient = productClient;
        this.cartClient = cartClient;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 查询同一幂等号是否已经创建过订单。
     *
     * @return 已存在时返回那笔订单的 ID；否则返回 null
     */
    private Long findIdempotentOrder(Long userId, String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }

        try {
            String key = IDEMPOTENT_KEY_PREFIX + userId + ":" + requestId;
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                return null;
            }

            return Long.valueOf(value);

        } catch (RuntimeException exception) {
            // 幂等是"防重复"的增强能力，不能因为它让下单整体不可用。
            // Redis 挂了就降级成"无幂等"，只记日志。
            log.warn(
                    "查询下单幂等标记失败，本次按非幂等处理，userId={}, requestId={}",
                    userId,
                    requestId,
                    exception
            );
            return null;
        }
    }

    /**
     * 记录本次下单的幂等结果。
     * 之后同一个幂等号的请求会直接返回这笔订单，不再重复创建。
     */
    private void saveIdempotentOrder(
            Long userId,
            String requestId,
            Long orderId) {

        if (requestId == null || requestId.isBlank()) {
            return;
        }

        try {
            String key = IDEMPOTENT_KEY_PREFIX + userId + ":" + requestId;

            redisTemplate.opsForValue().set(
                    key,
                    String.valueOf(orderId),
                    IDEMPOTENT_TTL
            );

        } catch (RuntimeException exception) {
            // 订单此时已经创建成功。
            // 绝对不能因为"写幂等标记失败"把整个事务回滚掉 ——
            // 那会变成"幂等这个附加功能把核心下单功能搞挂了"。
            log.warn(
                    "写入下单幂等标记失败，userId={}, requestId={}, orderId={}",
                    userId,
                    requestId,
                    orderId,
                    exception
            );
        }
    }

    /**
     * 删除幂等标记。
     * 订单因为后续步骤失败而被回滚时必须调用，
     * 否则用户重试会拿到一个已经不存在的订单 ID。
     */
    private void removeIdempotentOrder(Long userId, String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return;
        }

        try {
            redisTemplate.delete(
                    IDEMPOTENT_KEY_PREFIX + userId + ":" + requestId);

        } catch (RuntimeException exception) {
            log.warn(
                    "删除下单幂等标记失败，userId={}, requestId={}",
                    userId,
                    requestId,
                    exception
            );
        }
    }

    /**
     * 根据真实商品价格创建订单，返回新订单的 ID。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long userId, CreateOrderRequest request) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 幂等：同一幂等号重复提交时，直接返回第一次创建的订单 ID。
        // 不这样做的话，用户重复点击会创建多笔订单并多次扣库存。
        Long existingOrderId =
                findIdempotentOrder(userId, request.getRequestId());
        if (existingOrderId != null) {
            return existingOrderId;
        }

        // 先保存主表，MyBatis-Plus 插入后会回填订单 ID。
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo(generateOrderNo());
        orderInfo.setUserId(userId);
        orderInfo.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        orderInfo.setExpireTime(LocalDateTime.now().plusMinutes(30));
        orderInfo.setDeleted(0);

        // 初始订单金额先设置为 0
        orderInfo.setTotalAmount(BigDecimal.ZERO);

        orderInfoMapper.insert(orderInfo);

        // 记录已经成功扣减库存的商品，后面任何一步失败时用于补偿。
        List<OrderItemRequest> decreasedItems = new ArrayList<>();

        // 逐个查询真实商品信息，计算金额并保存订单明细。
        try {
            for (OrderItemRequest requestItem : request.getItems()) {
                ProductSnapshot product =
                        productClient.getOnSaleProduct(requestItem.getProductId());

                // 调用商品服务扣减库存
                productClient.decreaseStock(
                        requestItem.getProductId(),
                        requestItem.getQuantity()
                );

                // 扣减成功后立刻记账，这是补偿环节唯一的依据。
                decreasedItems.add(requestItem);

                BigDecimal subtotal = product.getPrice()
                        .multiply(BigDecimal.valueOf(requestItem.getQuantity()));

                totalAmount = totalAmount.add(subtotal);

                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(orderInfo.getId());
                orderItem.setProductId(product.getId());
                orderItem.setProductName(product.getName());
                orderItem.setPrice(product.getPrice());
                orderItem.setQuantity(requestItem.getQuantity());
                orderItem.setSubtotal(subtotal);
                orderItemMapper.insert(orderItem);
            }
        } catch (RuntimeException exception) {
            // 本地事务只能回滚订单库，回滚不了商品服务已经提交的扣减。
            // 这里显式调用商品服务，把前面扣成功的库存加回去，再继续抛出异常。
            compensateDecreasedStock(decreasedItems);
            throw exception;
        }

        // 订单明细全部写入成功后，再更新订单总金额。
        orderInfo.setTotalAmount(totalAmount);
        orderInfoMapper.updateById(orderInfo);

        // 走到这里才算下单成功，此时才记录幂等结果。
        // 中途失败不会写入，用户可以正常重试。
        saveIdempotentOrder(
                userId,
                request.getRequestId(),
                orderInfo.getId()
        );

        return orderInfo.getId();
    }


    /**
     * 查询当前用户的订单列表。
     */
    public List<OrderInfo> listMyOrders(Long userId) {
        LambdaQueryWrapper<OrderInfo> query = new LambdaQueryWrapper<>();

        query.eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getDeleted, 0)
                .orderByDesc(OrderInfo::getCreateTime);

        return orderInfoMapper.selectList(query);
    }

    /**
     * 生成本地开发阶段使用的唯一订单号。
     */
    private String generateOrderNo() {
        return "TK" + UUID.randomUUID().toString().replace("-", "");
    }


    /**
     * 查询当前用户的一笔订单详情。
     */
    public OrderDetailResponse getOrderDetail(Long userId, Long orderId) {
        // 先查询订单主表，并限制只能查询自己的订单
        LambdaQueryWrapper<OrderInfo> orderQuery = new LambdaQueryWrapper<>();

        orderQuery.eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getDeleted, 0);

        OrderInfo order = orderInfoMapper.selectOne(orderQuery);

        // 订单不存在，或不属于当前用户
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 根据订单 ID 查询商品明细
        LambdaQueryWrapper<OrderItem> itemQuery = new LambdaQueryWrapper<>();
        itemQuery.eq(OrderItem::getOrderId, orderId);

        OrderDetailResponse response = new OrderDetailResponse();
        response.setOrder(order);
        response.setItems(orderItemMapper.selectList(itemQuery));

        return response;
    }


    /**
     * 取消当前用户的一笔待支付订单。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId) {
        // 只能找到当前用户自己的、未删除的订单
        LambdaQueryWrapper<OrderInfo> query = new LambdaQueryWrapper<>();

        query.eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getDeleted, 0);

        OrderInfo order = orderInfoMapper.selectOne(query);

        // 订单不存在，或不属于当前用户
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 只有待支付订单才允许取消。
        // 用 409 而不是 400：这是"状态冲突"，不是"参数填错了"，
        // 前端要能据此提示"当前状态不允许取消"。
        if (!Integer.valueOf(OrderStatus.PENDING_PAYMENT.getCode())
                .equals(order.getStatus())) {
            throw new BusinessException(409, "只有待支付订单可以取消");
        }

        // 只有仍处于待支付状态时，才能抢占取消资格。
        LambdaUpdateWrapper<OrderInfo> update =
                new LambdaUpdateWrapper<>();

        update.eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getStatus,
                        OrderStatus.PENDING_PAYMENT.getCode())
                .eq(OrderInfo::getDeleted, 0)
                .set(OrderInfo::getStatus,
                        OrderStatus.CANCELLED.getCode());

        int affectedRows = orderInfoMapper.update(null, update);

        // 影响行数为 0，说明订单已被其他操作处理，不能重复恢复库存。
        if (affectedRows == 0) {
            throw new BusinessException(409, "订单状态已变化，请刷新后重试");
        }

        // 取消成功后，恢复该订单的全部商品库存。
        restoreStock(orderId);
    }


    /**
     * 模拟支付当前用户的一笔待支付订单。
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public void payOrder(Long userId, Long orderId) {
        // 只能查询当前用户自己的订单
        LambdaQueryWrapper<OrderInfo> query = new LambdaQueryWrapper<>();

        query.eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getDeleted, 0);

        OrderInfo order = orderInfoMapper.selectOne(query);

        // 订单不存在，或不属于当前用户
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 只有待支付订单可以支付
        if (!Integer.valueOf(OrderStatus.PENDING_PAYMENT.getCode())
                .equals(order.getStatus())) {
            throw new BusinessException(409, "只有待支付订单可以支付");
        }

        // 支付截止时间已到：标记过期并恢复已扣减的库存。
        if (!order.getExpireTime().isAfter(LocalDateTime.now())) {
            // 统一关闭过期订单并恢复库存。
            expireOrder(orderId);

            // 这里是"订单超时"，不是"参数填错了"。
            // 必须给前端一句能区分的提示，否则用户只会看到 invalid parameter。
            throw new BusinessException(400, "订单已超时关闭，请重新下单");
        }

        // 只有仍处于待支付状态时，才能改为已支付。
        LambdaUpdateWrapper<OrderInfo> update =
                new LambdaUpdateWrapper<>();

        update.eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getStatus,
                        OrderStatus.PENDING_PAYMENT.getCode())
                .eq(OrderInfo::getDeleted, 0)
                .set(OrderInfo::getStatus,
                        OrderStatus.PAID.getCode());

        int affectedRows = orderInfoMapper.update(null, update);

        // 如果影响行数为 0，说明订单已被取消或过期。
        if (affectedRows == 0) {
            throw new BusinessException(409, "订单状态已变化，请刷新后重试");
        }
    }



    /**
     * 将当前用户购物车中已选中的商品创建为订单。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createOrderFromCart(Long userId, String requestId) {
        // 幂等检查必须放在最前面，早于任何业务校验。
        // 原因：第一次结算成功后购物车已被清空，如果先查购物车，
        // 重复请求会撞到「没有已勾选商品」的 400，根本走不到
        // createOrder 里的幂等判断，用户看到的是一个莫名其妙的参数错误。
        Long existingOrderId =
                findIdempotentOrder(userId, requestId);
        if (existingOrderId != null) {
            return existingOrderId;
        }

        List<CartItemSnapshot> cartItems = cartClient.listItems(userId);
        List<OrderItemRequest> orderItems = new ArrayList<>();

        // 只把 checked = 1 的购物车商品转换为订单商品。
        for (CartItemSnapshot cartItem : cartItems) {
            if (Integer.valueOf(1).equals(cartItem.getChecked())) {
                OrderItemRequest orderItem = new OrderItemRequest();
                orderItem.setProductId(cartItem.getProductId());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItems.add(orderItem);
            }
        }

        // 没有选中商品时，不允许创建空订单。
        // 给一句能看懂的中文，别让前端只拿到 invalid parameter。
        if (orderItems.isEmpty()) {
            throw new BusinessException(400, "购物车里没有已勾选的商品");
        }

        // 复用已有创建订单逻辑，仍会查询商品服务的真实价格。
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(orderItems);
        // 把幂等号透传给 createOrder，让购物车结算同样具备防重复能力。
        request.setRequestId(requestId);

        // 先创建订单；失败时不会继续清理购物车。
        Long orderId = createOrder(userId, request);

        try {
            // 订单创建成功后，清理当前用户已结算的购物车商品。
            cartClient.deleteCheckedItems(userId);
        } catch (RuntimeException exception) {
            // 外层事务会回滚订单，但商品库存已经真实扣减，必须显式归还。
            restoreStock(orderId);
            // 订单即将被回滚，幂等标记也必须一起清掉，
            // 否则用户重试时会拿到一个已经不存在的订单 ID。
            removeIdempotentOrder(userId, requestId);
            throw exception;
        }

        return orderId;
    }

    /**
     * 确认收货，完成当前用户的一笔已支付订单。
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long userId, Long orderId) {
        // 只能查询当前用户自己的订单。
        LambdaQueryWrapper<OrderInfo> query = new LambdaQueryWrapper<>();

        query.eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getDeleted, 0);

        OrderInfo order = orderInfoMapper.selectOne(query);

        // 订单不存在，或不属于当前用户。
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 只有已支付订单可以确认收货。
        if (!Integer.valueOf(OrderStatus.PAID.getCode())
                .equals(order.getStatus())) {
            throw new BusinessException(409, "只有已支付订单可以确认收货");
        }

        // 和取消、支付保持一致，用条件更新抢占，
        // 避免并发重复确认收货。
        LambdaUpdateWrapper<OrderInfo> update =
                new LambdaUpdateWrapper<>();

        update.eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getStatus, OrderStatus.PAID.getCode())
                .eq(OrderInfo::getDeleted, 0)
                .set(OrderInfo::getStatus, OrderStatus.COMPLETED.getCode());

        int affectedRows = orderInfoMapper.update(null, update);

        // 影响行数为 0，说明订单状态在校验之后又被别的操作改了。
        if (affectedRows == 0) {
            throw new BusinessException(409, "订单状态已变化，请刷新后重试");
        }
    }



    /**
     * 条件关闭一笔已过期的待支付订单，并恢复库存。
     *
     * @return true 表示本次成功关闭；false 表示订单已被其他操作处理
     */
    public boolean expireOrder(Long orderId) {
        LambdaUpdateWrapper<OrderInfo> update =
                new LambdaUpdateWrapper<>();

        update.eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getStatus,
                        OrderStatus.PENDING_PAYMENT.getCode())
                .eq(OrderInfo::getDeleted, 0)
                .set(OrderInfo::getStatus,
                        OrderStatus.EXPIRED.getCode());

        int affectedRows = orderInfoMapper.update(null, update);

        // 只有抢占成功的请求才能恢复库存。
        if (affectedRows == 0) {
            return false;
        }

        restoreStock(orderId);
        return true;
    }

    /**
     * 根据订单明细逐项恢复库存。
     */
    private void restoreStock(Long orderId) {
        LambdaQueryWrapper<OrderItem> query = new LambdaQueryWrapper<>();
        query.eq(OrderItem::getOrderId, orderId);

        List<OrderItem> items = orderItemMapper.selectList(query);

        for (OrderItem item : items) {
            try {
                productClient.increaseStock(
                        item.getProductId(),
                        item.getQuantity()
                );
            } catch (RuntimeException exception) {
                // 记录失败明细，后续可根据日志进行补偿。
                log.error(
                        "恢复库存失败，orderId={}, productId={}, quantity={}",
                        orderId,
                        item.getProductId(),
                        item.getQuantity(),
                        exception
                );
            }
        }
    }



    /**
     * 下单失败时，把已经扣减成功的库存逐项加回去。
     * 这里只做尽力补偿：单件补偿失败会记录日志，不影响其余商品。
     */
    private void compensateDecreasedStock(
            List<OrderItemRequest> decreasedItems) {

        // 按相反顺序回滚，和扣减过程形成对称。
        for (int i = decreasedItems.size() - 1; i >= 0; i--) {
            OrderItemRequest item = decreasedItems.get(i);

            try {
                productClient.increaseStock(
                        item.getProductId(),
                        item.getQuantity()
                );
            } catch (RuntimeException exception) {
                // 补偿本身也可能失败，记录下来供后续人工或自动补偿。
                log.error(
                        "下单失败补偿库存失败，productId={}, quantity={}",
                        item.getProductId(),
                        item.getQuantity(),
                        exception
                );
            }
        }
    }

    /**
     * 管理员关闭一笔待支付订单，并恢复该订单占用的库存。
     * 已支付、已取消、已完成、已过期的订单都属于终态，不允许关闭。
     */
    @Transactional(rollbackFor = Exception.class)
    public void closeOrderByAdmin(Long orderId, Long operatorId) {
        LambdaUpdateWrapper<OrderInfo> update =
                new LambdaUpdateWrapper<>();

        // 条件更新：只有仍处于待支付状态时才能抢占关单资格。
        update.eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getStatus,
                        OrderStatus.PENDING_PAYMENT.getCode())
                .eq(OrderInfo::getDeleted, 0)
                .set(OrderInfo::getStatus,
                        OrderStatus.CANCELLED.getCode());

        int affectedRows = orderInfoMapper.update(null, update);

        // 影响行数为 0：订单不存在，或已经是终态。
        if (affectedRows == 0) {
            throw new BusinessException(
                    409,
                    "订单当前状态不允许关闭"
            );
        }

        // 抢占成功后恢复库存，和用户取消共用同一段逻辑。
        restoreStock(orderId);

        // 记录操作人，便于事后追溯是哪位管理员关的单。
        log.info("管理员关闭订单，orderId={}, operatorId={}",
                orderId, operatorId);
    }


    /**
     * 统计某商品还有多少笔待支付订单。
     * 供后台删除商品前校验：还有未完成订单的商品不应该被删除。
     */
    public int countPendingOrdersByProduct(Long productId) {
        return orderItemMapper
                .countPendingOrdersByProduct(productId);
    }

    /**
     * 查询全部订单，供后台管理使用。
     * 按 id 倒序只取最近 100 条，避免一次把全部历史拉出来。
     */
    public List<OrderInfo> listAllOrders() {
        LambdaQueryWrapper<OrderInfo> query =
                new LambdaQueryWrapper<>();

        query.eq(OrderInfo::getDeleted, 0)
                .orderByDesc(OrderInfo::getId)
                .last("LIMIT 100");

        return orderInfoMapper.selectList(query);
    }

    /**
     * 查询任意一笔订单的详情，供后台管理使用。
     * 与用户版 getOrderDetail 的唯一区别：不校验订单归属。
     */
    public OrderDetailResponse getOrderDetailForAdmin(Long orderId) {
        LambdaQueryWrapper<OrderInfo> orderQuery =
                new LambdaQueryWrapper<>();

        // 后台可以查任何用户的订单，所以这里没有 userId 条件。
        orderQuery.eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getDeleted, 0);

        OrderInfo order = orderInfoMapper.selectOne(orderQuery);

        // 订单不存在或已逻辑删除。
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        LambdaQueryWrapper<OrderItem> itemQuery =
                new LambdaQueryWrapper<>();
        itemQuery.eq(OrderItem::getOrderId, orderId);

        OrderDetailResponse response = new OrderDetailResponse();
        response.setOrder(order);
        response.setItems(orderItemMapper.selectList(itemQuery));

        return response;
    }


}
