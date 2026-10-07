package com.takej.mall.order.controller;

import com.takej.mall.common.Result;
import com.takej.mall.order.entity.OrderInfo;
import com.takej.mall.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.takej.mall.order.dto.CloseOrderRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.takej.mall.order.dto.OrderDetailResponse;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * 订单服务内部接口，只供其他微服务调用。
 */
@RestController
@RequestMapping("/internal/orders")
public class InternalOrderController {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 查询全部订单，供后台管理服务内部调用。
     */
    @GetMapping("/all")
    public Result<List<OrderInfo>> listAllOrders() {
        return Result.success(orderService.listAllOrders());
    }

    /**
     * 管理员关闭一笔待支付订单，供后台管理服务内部调用。
     */
    @PostMapping("/{orderId}/close")
    public Result<Void> closeOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody CloseOrderRequest request) {

        orderService.closeOrderByAdmin(
                orderId,
                request.getOperatorId()
        );

        return Result.success();
    }


    /**
     * 查询任意一笔订单的详情，供后台管理服务内部调用。
     */
    @GetMapping("/{orderId}")
    public Result<OrderDetailResponse> getOrderDetail(
            @PathVariable Long orderId) {

        return Result.success(
                orderService.getOrderDetailForAdmin(orderId)
        );
    }


    /**
     * 统计某商品的待支付订单数量，供后台删除商品前校验。
     */
    @GetMapping("/pending-count")
    public Result<Integer> countPendingOrders(
            @RequestParam Long productId) {

        return Result.success(
                orderService.countPendingOrdersByProduct(productId)
        );
    }

}