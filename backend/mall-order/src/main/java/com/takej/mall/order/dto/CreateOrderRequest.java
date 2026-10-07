package com.takej.mall.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 提交订单的请求参数。
 */
public class CreateOrderRequest {

    /** 本次下单的商品列表 */
    @NotEmpty(message = "订单商品不能为空")
    @Valid
    private List<OrderItemRequest> items;

    /**
     * 幂等号，由前端每次点击下单时生成（例如 UUID）。
     * 后端用它保证"同一请求只创建一笔订单"：
     * 重复提交会直接返回第一次创建的订单 ID，不会重复扣库存。
     * 不传则不做幂等，行为和以前一样。
     */
    private String requestId;

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}