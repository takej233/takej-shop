package com.takej.mall.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.takej.mall.order.service.OrderService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.takej.mall.order.entity.OrderInfo;
import com.takej.mall.order.enums.OrderStatus;
import com.takej.mall.order.mapper.OrderInfoMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时处理已过期但未支付的订单。
 */
@Component
public class OrderExpireTask {

        private static final Logger log =
                LoggerFactory.getLogger(OrderExpireTask.class);

        private final OrderInfoMapper orderInfoMapper;
        private final OrderService orderService;


        public OrderExpireTask(OrderInfoMapper orderInfoMapper,
                               OrderService orderService) {
            this.orderInfoMapper = orderInfoMapper;
            this.orderService = orderService;
        }


    /**
     * 每隔一分钟检查一次过期订单。
     */
    @Scheduled(fixedDelay = 60_000)
    public void cancelExpiredOrders() {
        LambdaQueryWrapper<OrderInfo> query =
                new LambdaQueryWrapper<>();

        query.eq(OrderInfo::getStatus,
                        OrderStatus.PENDING_PAYMENT.getCode())
                .eq(OrderInfo::getDeleted, 0)
                .lt(OrderInfo::getExpireTime, LocalDateTime.now());

        List<OrderInfo> expiredOrders =
                orderInfoMapper.selectList(query);

        // 逐笔处理，避免只改状态而遗漏库存恢复。
        for (OrderInfo order : expiredOrders) {
            try {
                orderService.expireOrder(order.getId());
            } catch (RuntimeException exception) {
                // 单笔订单失败不能影响本轮其他订单。
                log.error(
                        "处理过期订单失败，orderId={}",
                        order.getId(),
                        exception
                );
            }
        }
    }
}
