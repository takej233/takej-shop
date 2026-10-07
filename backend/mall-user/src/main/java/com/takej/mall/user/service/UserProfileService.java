package com.takej.mall.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.takej.mall.user.entity.UserProfile;
import com.takej.mall.user.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;
import com.takej.mall.common.BusinessException;
import com.takej.mall.common.ErrorCode;
import com.takej.mall.user.dto.UpdateUserProfileRequest;

/**
 * 用户资料业务服务。
 */
@Service
public class UserProfileService {

    private final UserProfileMapper userProfileMapper;

    public UserProfileService(UserProfileMapper userProfileMapper) {
        this.userProfileMapper = userProfileMapper;
    }




    /**
     * 为新注册用户创建默认资料。
     * 如果资料已经存在，则保持幂等，不重复创建。
     */
    public void createDefault(Long userId) {
        LambdaQueryWrapper<UserProfile> query =
                new LambdaQueryWrapper<>();

        // 这里不加 deleted 条件，避免逻辑删除记录造成重复数据。
        query.eq(UserProfile::getUserId, userId);

        UserProfile existing = userProfileMapper.selectOne(query);

        if (existing != null) {
            // 如果历史资料被逻辑删除，重新激活它。
            if (Integer.valueOf(1).equals(existing.getDeleted())) {
                existing.setDeleted(0);
                userProfileMapper.updateById(existing);
            }

            // 已有正常资料时直接返回，保证重复调用不会重复插入。
            return;
        }

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setNickname("用户" + userId);
        profile.setGender(0);
        profile.setDeleted(0);

        userProfileMapper.insert(profile);
    }
    /**
     * 根据认证服务传来的用户 ID 查询资料。
     */
    public UserProfile getByUserId(Long userId) {
        LambdaQueryWrapper<UserProfile> query = new LambdaQueryWrapper<>();
        query.eq(UserProfile::getUserId, userId)
                .eq(UserProfile::getDeleted, 0);

        return userProfileMapper.selectOne(query);
    }
    /**
     * 修改当前用户的资料。
     */
    public void updateByUserId(
            Long userId,
            UpdateUserProfileRequest request) {

        UserProfile profile = getByUserId(userId);

        // 历史账号可能还没有资料记录。
        // 这里必须先补建一份默认资料再更新，而不是直接抛 404：
        // 否则用户会陷入死循环 —— 查资料 404，想补全资料也 404，永远补不上。
        if (profile == null) {
            createDefault(userId);
            profile = getByUserId(userId);
        }

        profile.setNickname(request.getNickname());
        profile.setAvatar(request.getAvatar());
        profile.setGender(request.getGender());
        profile.setPhone(request.getPhone());

        // 根据主键更新数据库
        userProfileMapper.updateById(profile);
    }


}