package com.takej.mall.auth.controller;

import com.takej.mall.common.Result;
import org.springframework.web.bind.annotation.*;
import com.takej.mall.common.ErrorCode;
import com.takej.mall.common.BusinessException;
import com.takej.mall.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import com.takej.mall.auth.dto.RegisterRequest;
import com.takej.mall.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.takej.mall.auth.dto.LoginRequest;
import com.takej.mall.auth.dto.LoginRequest;
import com.takej.mall.auth.dto.LoginResponse;




@RestController
@RequestMapping("/api/auth")
public class AuthController {


    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("mall-auth is running");
    }

    @GetMapping("/error")
    public Result<Void> error() {
        throw new BusinessException(ErrorCode.PARAM_ERROR);
    }

    @PostMapping("/validate")
    public Result<String> validate(@Valid @RequestBody RegisterRequest request) {
        return Result.success("parameter is valid");
    }

    @PostMapping("/register")
    public Result<Long> register(
            @Valid @RequestBody RegisterRequest request) {
        Long userId = authService.register(request);
        return Result.success(userId);
    }

    /**
     * 用户登录：校验账号密码并返回 JWT。
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }


}