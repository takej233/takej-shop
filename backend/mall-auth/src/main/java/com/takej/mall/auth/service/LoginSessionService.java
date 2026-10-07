package com.takej.mall.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** 登录会话服务：把登录 Token 临时保存到 Redis。 */
@Service
public class LoginSessionService {

    private static final String KEY_PREFIX = "takej:login:token:";

    private final StringRedisTemplate redisTemplate;

    public LoginSessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 保存 Token，并设置与 JWT 一致的过期时间。 */
    public void save(String token, Long userId, long expireSeconds) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + token,
                String.valueOf(userId),
                Duration.ofSeconds(expireSeconds)
        );
    }
}
