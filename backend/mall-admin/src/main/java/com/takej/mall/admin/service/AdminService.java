package com.takej.mall.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.takej.mall.admin.entity.SysAdmin;
import com.takej.mall.admin.mapper.SysAdminMapper;
import org.springframework.stereotype.Service;
import com.takej.mall.admin.dto.AdminLoginRequest;
import com.takej.mall.common.BusinessException;
import com.takej.mall.common.ErrorCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.takej.mall.admin.dto.AdminLoginResponse;
import com.takej.mall.common.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;



/**
 * 后台管理员业务服务。
 */
@Service
public class AdminService {

    private final SysAdminMapper sysAdminMapper;
    private final AdminLoginSessionService
            adminLoginSessionService;
    /**
     * BCrypt 密码工具：只用于校验请求密码与数据库哈希是否匹配。
     */
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    /**
     * JWT 签名密钥：从本地配置读取，不能写死在代码中。
     */
    @Value("${takej.jwt.secret}")
    private String jwtSecret;

    /**
     * 管理员登录 Token 有效期，单位为秒。
     */
    @Value("${takej.jwt.expire-seconds}")
    private long jwtExpireSeconds;
    public AdminService(
            SysAdminMapper sysAdminMapper,
            AdminLoginSessionService adminLoginSessionService) {

        this.sysAdminMapper = sysAdminMapper;
        this.adminLoginSessionService = adminLoginSessionService;
    }

    /**
     * 按用户名查询可用的管理员。
     * 只有正常状态且未逻辑删除的管理员才允许进入后续登录校验。
     *
     * @param username 登录用户名
     * @return 可用管理员；未找到时返回 null
     */
    public SysAdmin findActiveByUsername(String username) {
        LambdaQueryWrapper<SysAdmin> query =
                new LambdaQueryWrapper<>();

        query.eq(SysAdmin::getUsername, username)
                .eq(SysAdmin::getStatus, 1)
                .eq(SysAdmin::getDeleted, 0);

        return sysAdminMapper.selectOne(query);
    }


    /**
     * 校验管理员登录身份并生成管理员 Token。
     * 用户名不存在、管理员被禁用或密码错误时，统一返回登录失败，
     * 避免泄露管理员账号是否存在。
     *
     * @param request 管理员登录请求
     * @return 登录成功后的安全响应，不包含密码或密码哈希
     */
    public AdminLoginResponse authenticate(AdminLoginRequest request) {
        SysAdmin admin = findActiveByUsername(request.getUsername());

        // 用户名不存在、已禁用或已删除时，都统一视为登录失败。
        if (admin == null
                || !passwordEncoder.matches(
                request.getPassword(),
                admin.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 管理员 Token 使用 ADMIN 身份类型，供网关后续区分权限边界。
        String token = JwtTokenUtil.createToken(
                admin.getId(),
                admin.getUsername(),
                JwtTokenUtil.PRINCIPAL_TYPE_ADMIN,
                jwtSecret,
                jwtExpireSeconds
        );

        // Redis 会话存在时，管理员 Token 才会被后续网关视为有效。
        adminLoginSessionService.save(
                token,
                admin.getId(),
                jwtExpireSeconds
        );

        return new AdminLoginResponse(
                admin.getId(),
                admin.getUsername(),
                token
        );
    }


}