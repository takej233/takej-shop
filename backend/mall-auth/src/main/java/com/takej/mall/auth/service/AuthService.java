package com.takej.mall.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.takej.mall.auth.dto.RegisterRequest;
import com.takej.mall.auth.entity.SysUser;
import com.takej.mall.auth.mapper.SysUserMapper;
import com.takej.mall.common.BusinessException;
import com.takej.mall.common.ErrorCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.takej.mall.auth.dto.LoginRequest;
import com.takej.mall.auth.dto.LoginResponse;
import com.takej.mall.common.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import com.takej.mall.auth.client.UserProfileClient;


@Service
public class AuthService {


    private final SysUserMapper sysUserMapper;
    private final LoginSessionService loginSessionService;
    private final UserProfileClient userProfileClient;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public AuthService(SysUserMapper sysUserMapper,
                       LoginSessionService loginSessionService,
                       UserProfileClient userProfileClient) {
        this.sysUserMapper = sysUserMapper;
        this.loginSessionService = loginSessionService;
        this.userProfileClient = userProfileClient;
    }

    @Value("${takej.jwt.secret}")
    private String jwtSecret;

    @Value("${takej.jwt.expire-seconds}")
    private long jwtExpireSeconds;


    public Long register(RegisterRequest request) {
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();
        query.eq(SysUser::getUsername, request.getUsername());

        if (sysUserMapper.selectCount(query) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);
        user.setDeleted(0);

        sysUserMapper.insert(user);

        // 认证用户创建成功后，通知用户服务创建默认资料。
        userProfileClient.createDefault(user.getId());

        return user.getId();
    }

    /**
     * 校验账号密码并生成登录 Token。
     */
    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();
        query.eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getDeleted, 0);

        SysUser user = sysUserMapper.selectOne(query);

        if (user == null
                || !passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 账号被禁用。
        // 用 401 而不是 403：禁用账号是"身份不被承认"，
        // 而 403 会让前端提示"没有权限"，语义完全不对。
        // 前端在登录页收到 401 时不会跳转，只会展示这里的提示。
        if (user.getStatus() == 0) {
            throw new BusinessException(401, "账号已被禁用，请联系管理员");
        }

        String token = JwtTokenUtil.createToken(
                user.getId(),
                user.getUsername(),
                jwtSecret,
                jwtExpireSeconds
        );

        // 登录成功后记录会话，过期时间与 JWT 保持一致。
        loginSessionService.save(token, user.getId(), jwtExpireSeconds);

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                token
        );
    }



}
