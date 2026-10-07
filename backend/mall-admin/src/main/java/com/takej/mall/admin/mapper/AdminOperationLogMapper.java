package com.takej.mall.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takej.mall.admin.entity.AdminOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台管理员操作日志数据访问接口。
 */
@Mapper
public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLog> {
}