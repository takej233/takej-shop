package com.takej.mall.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 用户资料服务客户端。
 */
@Service
public class UserProfileClient {

    private final RestClient restClient;

    @Value("${takej.user.base-url}")
    private String userBaseUrl;

    public UserProfileClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * 调用用户服务创建默认资料。
     */
    public void createDefault(Long userId) {
        restClient.post()
                .uri(userBaseUrl + "/internal/user-profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("userId", userId))
                .retrieve()
                .toBodilessEntity();
    }
}