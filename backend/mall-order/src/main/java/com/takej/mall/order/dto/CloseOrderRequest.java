package com.takej.mall.order.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 管理员关闭订单的请求参数。
 */
public class CloseOrderRequest {

    /** 操作管理员 ID，由后台服务从网关注入的身份取出。 */
    @NotNull(message = "操作管理员不能为空")
    private Long operatorId;

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }
}