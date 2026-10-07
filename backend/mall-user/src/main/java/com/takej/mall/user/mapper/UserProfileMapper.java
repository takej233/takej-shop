package com.takej.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takej.mall.user.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户资料数据访问接口。
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}