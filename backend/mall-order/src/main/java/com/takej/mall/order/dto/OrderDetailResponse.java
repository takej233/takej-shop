package com.takej.mall.order.dto;

import com.takej.mall.order.entity.OrderInfo;
import com.takej.mall.order.entity.OrderItem;

import java.util.List;

/**
 * 订单详情响应：包含订单主信息和商品明细。
 */
public class OrderDetailResponse {

    /** 订单主表信息 */
    private OrderInfo order;

    /** 订单商品明细 */
    private List<OrderItem> items;

    public OrderInfo getOrder() {
        return order;
    }

    public void setOrder(OrderInfo order) {
        this.order = order;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}