package com.takej.mall.admin.service;

import com.takej.mall.admin.mapper.SysRoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import com.takej.mall.common.BusinessException;


/**
 * 后台权限查询服务。
 */
@Service
public class AdminPermissionService {

    private final SysRoleMapper sysRoleMapper;

    public AdminPermissionService(SysRoleMapper sysRoleMapper) {
        this.sysRoleMapper = sysRoleMapper;
    }

    /**
     * 查询某个管理员拥有的全部权限码。
     *
     * @param adminId 管理员 ID
     * @return 权限码列表，可能为空
     */
    public List<String> listPermissionCodes(Long adminId) {
        return sysRoleMapper
                .selectPermissionCodesByAdminId(adminId);
    }

    /**
     * 校验管理员是否拥有指定权限，没有则抛 403。
     *
     * @param adminId        管理员 ID
     * @param permissionCode 权限码
     */
    public void requirePermission(
            Long adminId,
            String permissionCode) {

        List<String> permissionCodes =
                listPermissionCodes(adminId);

        if (!permissionCodes.contains(permissionCode)) {
            throw new BusinessException(
                    403,
                    "没有该操作的权限：" + permissionCode
            );
        }
    }
}