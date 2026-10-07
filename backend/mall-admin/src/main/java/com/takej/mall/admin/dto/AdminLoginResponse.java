package com.takej.mall.admin.dto;

/**
 * 后台管理员登录成功后的响应数据。
 * 不包含密码或密码哈希等敏感字段。
 */
public class AdminLoginResponse {

    /** 管理员 ID。 */
    private Long adminId;

    /** 管理员用户名。 */
    private String username;

    /** 后续登录认证使用的 Token。 */
    private String token;

    public AdminLoginResponse(
            Long adminId,
            String username,
            String token) {

        this.adminId = adminId;
        this.username = username;
        this.token = token;
    }

    public Long getAdminId() {
        return adminId;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }
}