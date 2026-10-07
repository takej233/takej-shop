package com.takej.mall.admin.controller;

import com.takej.mall.admin.dto.AdminLoginRequest;
import com.takej.mall.admin.dto.AdminLoginResponse;
import com.takej.mall.admin.service.AdminService;
import com.takej.mall.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.takej.mall.admin.service.AdminLoginSessionService;
import com.takej.mall.common.BusinessException;
import com.takej.mall.common.ErrorCode;
import org.springframework.web.bind.annotation.RequestHeader;


/**
 * 后台管理员认证接口。
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminService adminService;
    private final AdminLoginSessionService
            adminLoginSessionService;

    public AdminAuthController(
            AdminService adminService,
            AdminLoginSessionService adminLoginSessionService) {

        this.adminService = adminService;
        this.adminLoginSessionService = adminLoginSessionService;
    }
    /**
     * 管理员登录：校验身份并返回管理员 Token。
     */
    @PostMapping("/login")
    public Result<AdminLoginResponse> login(
            @Valid @RequestBody AdminLoginRequest request) {

        return Result.success(adminService.authenticate(request));
    }


    /**
     * 管理员退出登录：删除 Redis 会话，让当前 Token 立即失效。
     */
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        // 网关已经校验过身份；这里再兜一层，避免缺请求头时抛出框架异常。
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 只删除当前这一个 Token 的会话，其他设备的登录不受影响。
        adminLoginSessionService.remove(
                authorization.substring(7).trim()
        );

        return Result.success();
    }


}