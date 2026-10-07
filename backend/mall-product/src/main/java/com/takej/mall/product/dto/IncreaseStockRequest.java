package com.takej.mall.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 商品恢复库存的请求参数。
 */
public class IncreaseStockRequest {

    /**
     * 本次需要恢复的数量，最少为 1。
     */
    @NotNull(message = "恢复数量不能为空")
    @Min(value = 1, message = "恢复数量必须大于 0")
    private Integer quantity;

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}