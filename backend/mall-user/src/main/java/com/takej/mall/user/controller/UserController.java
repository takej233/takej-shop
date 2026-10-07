package com.takej.mall.user.controller;

import com.takej.mall.common.Result;
import com.takej.mall.user.entity.UserProfile;
import com.takej.mall.user.service.UserProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.takej.mall.user.dto.UpdateUserProfileRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.takej.mall.common.BusinessException;
import com.takej.mall.common.ErrorCode;


/**
 * 用户资料接口。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * 查询当前登录用户的资料。
     */
    @GetMapping("/me")
    public Result<UserProfile> currentUser(
            @RequestHeader("X-User-Id") String userId) {

        UserProfile profile = userProfileService.getByUserId(
                Long.valueOf(userId));

// 资料不存在时返回 404，避免前端拿到 data=null 的空成功响应。
        if (profile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        return Result.success(profile);
    }

    /**
     * 修改当前登录用户的资料。
     */
    @PutMapping("/me")
    public Result<Void> updateCurrentUser(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateUserProfileRequest request) {

        userProfileService.updateByUserId(
                Long.valueOf(userId),
                request
        );

        return Result.success();
    }
}