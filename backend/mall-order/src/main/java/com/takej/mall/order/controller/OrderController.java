package com.takej.mall.order.controller;

import com.takej.mall.order.dto.CreateOrderRequest;
import com.takej.mall.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.takej.mall.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import com.takej.mall.order.entity.OrderInfo;
import java.util.List;
import com.takej.mall.order.dto.OrderDetailResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;




/**
 * 订单控制器：接收前端的订单请求。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * 构造方法注入订单业务类。
     */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单。
     *
     * @param userId  网关从 Token 解析后传入的用户 ID
     * @param request 前端提交的商品和数量
     */
    @PostMapping
    public Result<Long> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateOrderRequest request) {

        Long orderId = orderService.createOrder(
                Long.valueOf(userId),
                request
        );

        return Result.success(orderId);
    }

    /**
     * 查询当前登录用户的订单。
     */
    @GetMapping
    public Result<List<OrderInfo>> listMyOrders(
            @RequestHeader("X-User-Id") String userId) {

        List<OrderInfo> orders = orderService.listMyOrders(
                Long.valueOf(userId)
        );

        return Result.success(orders);
    }


    /**
     * 查询当前用户的一笔订单详情。
     */
    @GetMapping("/{orderId}")
    public Result<OrderDetailResponse> getOrderDetail(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long orderId) {

        OrderDetailResponse response = orderService.getOrderDetail(
                Long.valueOf(userId),
                orderId
        );

        return Result.success(response);
    }


    /**
     * 取消当前用户的一笔待支付订单。
     */
    @PutMapping("/{orderId}/cancel")
    public Result<Void> cancelOrder(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long orderId) {

        orderService.cancelOrder(
                Long.valueOf(userId),
                orderId
        );

        return Result.success(null);
    }


    /**
     * 模拟支付当前用户的一笔待支付订单。
     */
    @PostMapping("/{orderId}/pay")
    public Result<Void> payOrder(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long orderId) {

        orderService.payOrder(
                Long.valueOf(userId),
                orderId
        );

        return Result.success(null);
    }


    /**
     * 将当前用户已选中的购物车商品创建为订单。
     *
     * @param requestId 可选的幂等号，由前端每次点击结算时生成。
     *                  同一幂等号重复提交会返回第一次创建的订单，不会重复扣库存。
     */
    @PostMapping("/from-cart")
    public Result<Long> createOrderFromCart(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) String requestId) {

        Long orderId = orderService.createOrderFromCart(
                Long.valueOf(userId),
                requestId
        );

        return Result.success(orderId);
    }

    /**
     * 确认收货，完成当前用户的一笔已支付订单。
     */
    @PutMapping("/{orderId}/complete")
    public Result<Void> completeOrder(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long orderId) {

        orderService.completeOrder(
                Long.valueOf(userId),
                orderId
        );

        return Result.success(null);
    }


}