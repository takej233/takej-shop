package com.takej.mall.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 后台订单详情：订单主信息加商品明细。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminOrderDetail {

    /** 订单主信息，复用列表项的字段定义。 */
    private AdminOrderItem order;

    /** 商品明细。 */
    private List<AdminOrderLine> items;

    public AdminOrderItem getOrder() {
        return order;
    }

    public void setOrder(AdminOrderItem order) {
        this.order = order;
    }

    public List<AdminOrderLine> getItems() {
        return items;
    }

    public void setItems(List<AdminOrderLine> items) {
        this.items = items;
    }
}