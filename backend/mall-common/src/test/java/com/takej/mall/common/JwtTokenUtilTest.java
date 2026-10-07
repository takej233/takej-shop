package com.takej.mall.common;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** JWT 工具类测试：验证 Token 可以生成并正确解析。 */
class JwtTokenUtilTest {

    private static final String SECRET =
            "takej-local-jwt-secret-key-must-be-at-least-32";

    @Test
    void shouldParseToken() {
        String token = JwtTokenUtil.createToken(
                1L,
                "takej",
                SECRET,
                7200
        );

        Claims claims = JwtTokenUtil.parseToken(token, SECRET);

        assertEquals("1", claims.getSubject());
        assertEquals("takej", claims.get("username"));
        assertEquals(
                JwtTokenUtil.PRINCIPAL_TYPE_USER,
                claims.get(JwtTokenUtil.CLAIM_PRINCIPAL_TYPE)
        );
    }

    /**
     * 管理员 Token 必须携带 ADMIN 身份类型。
     */
    @Test
    void shouldCreateAdminToken() {
        String token = JwtTokenUtil.createToken(
                1L,
                "admin",
                JwtTokenUtil.PRINCIPAL_TYPE_ADMIN,
                SECRET,
                7200
        );

        Claims claims = JwtTokenUtil.parseToken(token, SECRET);

        assertEquals("1", claims.getSubject());
        assertEquals("admin", claims.get("username"));
        assertEquals(
                JwtTokenUtil.PRINCIPAL_TYPE_ADMIN,
                claims.get(JwtTokenUtil.CLAIM_PRINCIPAL_TYPE)
        );
    }
}
