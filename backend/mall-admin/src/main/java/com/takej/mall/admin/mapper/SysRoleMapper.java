package com.takej.mall.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takej.mall.admin.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 后台角色数据访问接口。
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询某个管理员通过角色获得的全部权限码。
     * 只统计启用且未删除的角色；
     * 同一个权限码被多个角色重复授予时，DISTINCT 保证只返回一次。
     */
    @Select("""
            SELECT DISTINCT rp.permission_code
            FROM sys_admin_role ar
            JOIN sys_role r ON r.id = ar.role_id
            JOIN sys_role_permission rp ON rp.role_id = r.id
            WHERE ar.admin_id = #{adminId}
              AND r.status = 1
              AND r.deleted = 0
            """)
    List<String> selectPermissionCodesByAdminId(
            @Param("adminId") Long adminId);
}