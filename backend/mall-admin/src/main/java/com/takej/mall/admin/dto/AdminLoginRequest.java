package com.takej.mall.admin.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台管理员登录请求参数。
 */
public class AdminLoginRequest {

    /** 管理员登录用户名。 */
    @NotBlank(message = "管理员用户名不能为空")
    private String username;

    /** 管理员登录密码。 */
    @NotBlank(message = "管理员密码不能为空")
    @Size(min = 6, message = "管理员密码至少为 6 位")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}