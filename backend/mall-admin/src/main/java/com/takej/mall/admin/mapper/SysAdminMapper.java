package com.takej.mall.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takej.mall.admin.entity.SysAdmin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台管理员数据访问接口。
 */
@Mapper
public interface SysAdminMapper extends BaseMapper<SysAdmin> {
}