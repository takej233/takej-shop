package com.takej.mall.order.dto;

/**
 * 购物车服务返回给订单服务的一项购物车数据。
 */
public class CartItemSnapshot {

    /** 商品 ID。 */
    private Long productId;

    /** 购买数量。 */
    private Integer quantity;

    /** 是否选中：0 未选中，1 已选中。 */
    private Integer checked;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getChecked() {
        return checked;
    }

    public void setChecked(Integer checked) {
        this.checked = checked;
    }
}