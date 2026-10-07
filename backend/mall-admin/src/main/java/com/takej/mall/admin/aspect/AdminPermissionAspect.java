package com.takej.mall.admin.aspect;

import com.takej.mall.admin.annotation.RequirePermission;
import com.takej.mall.admin.service.AdminPermissionConstants;
import com.takej.mall.admin.service.AdminPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 后台权限校验切面。
 * 拦截标注了 @RequirePermission 的方法，在方法执行前校验权限。
 */
@Aspect
@Component
public class AdminPermissionAspect {

    private final AdminPermissionService adminPermissionService;

    public AdminPermissionAspect(
            AdminPermissionService adminPermissionService) {

        this.adminPermissionService = adminPermissionService;
    }

    /**
     * 在目标方法执行前校验当前管理员是否拥有所需权限。
     * 校验不通过时抛出 BusinessException，由全局异常处理器统一返回 403。
     */
    @Before("@annotation(requirePermission)")
    public void checkPermission(
            RequirePermission requirePermission) {

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder
                        .getRequestAttributes();

        // 被拦截的方法总是由 HTTP 请求触发的，这里只是防御性判断。
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();

        String adminId = request.getHeader(
                AdminPermissionConstants.HEADER_ADMIN_ID
        );

        adminPermissionService.requirePermission(
                Long.valueOf(adminId),
                requirePermission.value()
        );
    }
}