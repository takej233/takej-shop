package com.takej.mall.cart.client;

import com.takej.mall.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 商品服务客户端。
 * 购物车在写入数据之前，通过它确认商品仍然存在且处于上架状态。
 */
@Service
public class ProductClient {

    private final RestClient restClient;

    /** 商品服务地址，从配置读取，不写死在代码里。 */
    @Value("${takej.product.base-url}")
    private String productBaseUrl;

    public ProductClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * 校验商品当前可以购买。
     * 商品不存在、已下架或已逻辑删除时，商品服务统一返回 404。
     *
     * @param productId 商品 ID
     */
    public void requireOnSale(Long productId) {
        try {
            restClient.get()
                    .uri(productBaseUrl + "/api/products/{id}", productId)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException exception) {
            // 商品服务用 404 表达"查不到可购买的商品"。
            if (exception.getStatusCode().value() == 404) {
                throw new BusinessException(
                        404,
                        "商品不存在或已下架"
                );
            }

            // 其他错误（比如商品服务没启动）保持原样往上抛，
            // 交给全局异常处理器变成 500，不要伪装成 404。
            throw exception;
        }
    }
}
