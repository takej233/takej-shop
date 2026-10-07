package com.takej.mall.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 商品扣减库存的请求参数。
 */
public class DecreaseStockRequest {

    /**
     * 本次需要扣减的数量，最少为 1。
     */
    @NotNull(message = "扣减数量不能为空")
    @Min(value = 1, message = "扣减数量必须大于 0")
    private Integer quantity;

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}