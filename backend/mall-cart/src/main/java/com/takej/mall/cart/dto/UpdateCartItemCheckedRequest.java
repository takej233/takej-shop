package com.takej.mall.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 修改购物车商品是否选中的请求参数。
 */
public class UpdateCartItemCheckedRequest {

    /** 是否选中：0 未选中，1 已选中。 */
    @NotNull(message = "选中状态不能为空")
    @Min(value = 0, message = "选中状态只能是 0 或 1")
    @Max(value = 1, message = "选中状态只能是 0 或 1")
    private Integer checked;

    public Integer getChecked() {
        return checked;
    }

    public void setChecked(Integer checked) {
        this.checked = checked;
    }
}