package com.takej.mall.admin.dto;

/**
 * 当前登录管理员的身份信息。
 * 不包含密码、密码哈希或登录 Token。
 */
public class AdminIdentityResponse {

    /** 管理员 ID。 */
    private Long adminId;

    /** 管理员用户名。 */
    private String username;

    public AdminIdentityResponse(
            Long adminId,
            String username) {

        this.adminId = adminId;
        this.username = username;
    }

    public Long getAdminId() {
        return adminId;
    }

    public String getUsername() {
        return username;
    }
}