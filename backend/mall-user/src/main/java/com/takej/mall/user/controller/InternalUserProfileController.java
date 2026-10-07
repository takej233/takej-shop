package com.takej.mall.user.controller;

import com.takej.mall.common.Result;
import com.takej.mall.user.dto.CreateUserProfileRequest;
import com.takej.mall.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户资料服务内部接口，只供认证服务调用。
 */
@RestController
@RequestMapping("/internal/user-profiles")
public class InternalUserProfileController {

    private final UserProfileService userProfileService;

    public InternalUserProfileController(
            UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * 为新注册用户创建默认资料。
     */
    @PostMapping
    public Result<Void> createDefault(
            @Valid @RequestBody CreateUserProfileRequest request) {

        userProfileService.createDefault(request.getUserId());

        return Result.success();
    }
}