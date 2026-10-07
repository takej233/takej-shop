package com.takej.mall.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 后台人工调整商品库存的请求参数。
 * 故意不包含 operatorId：操作管理员必须来自网关注入的身份。
 */
public class AdminAdjustStockRequest {

    /** 变动数量，正数为增加，负数为减少，不能为 0。 */
    @NotNull(message = "调整数量不能为空")
    private Integer changeQuantity;

    /** 调整原因，便于事后追溯。 */
    @Size(max = 255, message = "调整原因不能超过255个字符")
    private String reason;

    public Integer getChangeQuantity() {
        return changeQuantity;
    }

    public void setChangeQuantity(Integer changeQuantity) {
        this.changeQuantity = changeQuantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}