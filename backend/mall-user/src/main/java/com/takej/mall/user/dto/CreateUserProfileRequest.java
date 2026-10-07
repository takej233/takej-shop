package com.takej.mall.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 创建用户资料的请求参数。
 */
public class CreateUserProfileRequest {

    /**
     * 认证服务生成的用户 ID。
     */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}