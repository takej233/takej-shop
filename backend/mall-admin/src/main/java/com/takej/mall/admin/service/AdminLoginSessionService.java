package com.takej.mall.admin.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 后台管理员登录会话服务。
 * 使用独立 Redis Key 前缀，避免和普通用户登录会话混用。
 */
@Service
public class AdminLoginSessionService {

    private static final String KEY_PREFIX =
            "takej:admin:login:token:";

    private final StringRedisTemplate redisTemplate;

    public AdminLoginSessionService(
            StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 保存管理员 Token，并让 Redis 过期时间与 JWT 保持一致。
     *
     * @param token         管理员登录 Token
     * @param adminId       当前管理员 ID
     * @param expireSeconds Token 有效期，单位为秒
     */
    public void save(
            String token,
            Long adminId,
            long expireSeconds) {

        redisTemplate.opsForValue().set(
                KEY_PREFIX + token,
                String.valueOf(adminId),
                Duration.ofSeconds(expireSeconds)
        );
    }

    /**
     * 删除管理员登录会话，使当前 Token 立即失效。
     * Key 不存在时删除是空操作，因此重复调用是安全的。
     *
     * @param token 管理员登录 Token
     */
    public void remove(String token) {
        redisTemplate.delete(KEY_PREFIX + token);
    }


}