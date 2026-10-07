package com.takej.mall.order.client;

import com.takej.mall.common.Result;
import com.takej.mall.order.dto.CartItemSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

/**
 * 购物车服务客户端：订单服务通过它读取购物车数据。
 */
@Service
public class CartClient {

    private final RestClient restClient;

    @Value("${takej.cart.base-url}")
    private String cartBaseUrl;

    public CartClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * 查询指定用户的购物车项。
     */
    public List<CartItemSnapshot> listItems(Long userId) {
        Result<List<CartItemSnapshot>> result = restClient.get()
                .uri(cartBaseUrl + "/api/cart/items")
                .header("X-User-Id", String.valueOf(userId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        // 购物车为空时，返回空列表，避免后续空指针错误。
        if (result == null || result.getData() == null) {
            return Collections.emptyList();
        }

        return result.getData();
    }

    /**
     * 删除指定用户所有已选中的购物车商品。
     */
    public void deleteCheckedItems(Long userId) {
        restClient.delete()
                .uri(cartBaseUrl + "/api/cart/items/selected")
                .header("X-User-Id", String.valueOf(userId))
                .retrieve()
                .toBodilessEntity();
    }



}