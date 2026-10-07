package com.takej.mall.gateway.filter;

import com.takej.mall.common.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 网关 JWT 全局过滤器：统一检查请求中的登录 Token。
 */
@Component
public class JwtGlobalFilter
        implements org.springframework.cloud.gateway.filter.GlobalFilter,
        org.springframework.core.Ordered {

    /** 普通用户登录会话 Redis Key 前缀。 */
    private static final String USER_REDIS_KEY_PREFIX =
            "takej:login:token:";

    /** 后台管理员登录会话 Redis Key 前缀。 */
    private static final String ADMIN_REDIS_KEY_PREFIX =
            "takej:admin:login:token:";

    /** 后台管理接口统一路径前缀。 */
    private static final String ADMIN_API_PREFIX = "/api/admin/";

    // 不需要登录即可访问的接口
    private static final Set<String> WHITE_LIST = Set.of(
            "/health",
            "/api/auth/ping",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/validate",
            "/api/admin/auth/login"
    );

    private final StringRedisTemplate redisTemplate;

    @Value("${takej.jwt.secret}")
    private String jwtSecret;

    public JwtGlobalFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // 登录、注册等白名单接口不需要携带 Token。
        if (WHITE_LIST.contains(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        // 检查 Authorization: Bearer Token。
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = authorization.substring(7).trim();

        try {
            // 验证 JWT 签名和有效期。
            Claims claims = JwtTokenUtil.parseToken(token, jwtSecret);

            String principalType = claims.get(
                    JwtTokenUtil.CLAIM_PRINCIPAL_TYPE,
                    String.class
            );

            // 后台接口只能使用 ADMIN Token 和管理员 Redis 会话。
            if (path.startsWith(ADMIN_API_PREFIX)) {
                if (!JwtTokenUtil.PRINCIPAL_TYPE_ADMIN
                        .equals(principalType)) {
                    return unauthorized(exchange);
                }

                Boolean exists = redisTemplate.hasKey(
                        ADMIN_REDIS_KEY_PREFIX + token
                );

                if (!Boolean.TRUE.equals(exists)) {
                    return unauthorized(exchange);
                }

                // 清除客户端伪造的用户请求头，只注入真实管理员身份。
                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(request -> request.headers(headers -> {
                            headers.remove("X-User-Id");
                            headers.remove("X-Username");
                            headers.set("X-Admin-Id",
                                    claims.getSubject());
                            headers.set("X-Admin-Username",
                                    claims.get(
                                            "username",
                                            String.class
                                    ));
                        }))
                        .build();

                return chain.filter(mutatedExchange);
            }

            // 原有商城接口只能使用 USER Token 和普通用户 Redis 会话。
            if (!JwtTokenUtil.PRINCIPAL_TYPE_USER
                    .equals(principalType)) {
                return unauthorized(exchange);
            }

            Boolean exists = redisTemplate.hasKey(
                    USER_REDIS_KEY_PREFIX + token
            );

            if (!Boolean.TRUE.equals(exists)) {
                return unauthorized(exchange);
            }

            // 清除客户端伪造的管理员请求头，只注入真实普通用户身份。
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(request -> request.headers(headers -> {
                        headers.remove("X-Admin-Id");
                        headers.remove("X-Admin-Username");
                        headers.set("X-User-Id", claims.getSubject());
                        headers.set("X-Username",
                                claims.get("username", String.class));
                    }))
                    .build();

            return chain.filter(mutatedExchange);

        } catch (Exception exception) {
            return unauthorized(exchange);
        }
    }

    /**
     * 返回 401 未认证。
     * 这里刻意写成和业务服务一致的 Result 结构：
     * 之前只设状态码、不写响应体，前端拿不到任何可读信息，
     * 只能自己兜一句"登录已失效"。现在前端可以统一从 message 取提示。
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"code\":401,"
                + "\"message\":\"未登录或登录已失效\","
                + "\"data\":null}";

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}