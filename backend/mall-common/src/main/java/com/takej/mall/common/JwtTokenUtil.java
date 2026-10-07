package com.takej.mall.common;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import io.jsonwebtoken.Claims;





public final class JwtTokenUtil {

    /** JWT 声明中保存身份类型的字段名。 */
    public static final String CLAIM_PRINCIPAL_TYPE =
            "principalType";

    /** 普通商城用户身份类型。 */
    public static final String PRINCIPAL_TYPE_USER = "USER";

    /** 后台管理员身份类型。 */
    public static final String PRINCIPAL_TYPE_ADMIN = "ADMIN";


    private JwtTokenUtil() {
    }

    /**
     * 创建普通用户 Token。
     * 保留原有方法签名，避免已有认证服务调用受到影响。
     */
    public static String createToken(
            Long userId,
            String username,
            String secret,
            long expireSeconds) {

        return createToken(
                userId,
                username,
                PRINCIPAL_TYPE_USER,
                secret,
                expireSeconds
        );
    }

    /**
     * 创建带身份类型的 Token。
     *
     * @param principalId   当前登录主体 ID，可以是用户 ID 或管理员 ID
     * @param username      登录用户名
     * @param principalType 身份类型，例如 USER、ADMIN
     * @param secret        JWT 签名密钥
     * @param expireSeconds Token 有效期（秒）
     * @return 已签名的 JWT
     */
    public static String createToken(
            Long principalId,
            String username,
            String principalType,
            String secret,
            long expireSeconds) {

        SecretKey key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8));

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(principalId))
                .claim("username", username)
                .claim(CLAIM_PRINCIPAL_TYPE, principalType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key)
                .compact();
    }


    /**
     * 解析并验证 JWT Token。
     *
     * @param token 客户端提交的 Token
     * @param secret JWT 签名密钥
     * @return Token 中保存的声明信息
     */
    public static Claims parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


}