package com.takej.mall.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 修改商品上下架状态的请求参数。
 */
public class UpdateProductStatusRequest {

    /** 商品状态：0 下架，1 上架。 */
    @NotNull(message = "商品状态不能为空")
    @Min(value = 0, message = "商品状态只能是 0 或 1")
    @Max(value = 1, message = "商品状态只能是 0 或 1")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}