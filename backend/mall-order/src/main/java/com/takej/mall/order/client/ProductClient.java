package com.takej.mall.order.client;

import com.takej.mall.common.BusinessException;
import com.takej.mall.common.ErrorCode;
import com.takej.mall.common.Result;
import com.takej.mall.order.dto.ProductSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.MediaType;
import java.util.Map;


/**
 * 商品服务客户端：订单服务通过它查询商品真实信息。
 */
@Service
public class ProductClient {

    private final RestClient restClient;

    @Value("${takej.product.base-url}")
    private String productBaseUrl;

    public ProductClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * 查询可购买商品。
     */
    public ProductSnapshot getOnSaleProduct(Long productId) {
        try {
            Result<ProductSnapshot> result = restClient.get()
                    .uri(productBaseUrl + "/api/products/{id}", productId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (result == null || result.getData() == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }

            return result.getData();

        } catch (RestClientResponseException exception) {
            // 商品服务返回 404，表示商品不存在、已下架或已删除
            if (exception.getStatusCode().value() == 404) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }

            throw exception;
        }
    }




    /**
     * 调用商品服务，扣减指定商品的库存。
     * 将商品服务的业务错误转换为订单服务自己的业务异常。
     */
    public void decreaseStock(Long productId, Integer quantity) {
        try {
            restClient.post()
                    .uri(productBaseUrl
                                    + "/internal/products/{productId}/decrease-stock",
                            productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("quantity", quantity))
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            // 商品不存在、已下架或已删除。
            if (status == 404) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }

            // 商品库存不足。
            if (status == 409) {
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
            }

            // 其他 HTTP 错误继续交给上层处理。
            throw exception;
        }
    }


    /**
     * 调用商品服务，恢复指定商品的库存。
     */
    public void increaseStock(Long productId, Integer quantity) {
        restClient.post()
                .uri(productBaseUrl
                                + "/internal/products/{productId}/increase-stock",
                        productId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("quantity", quantity))
                .retrieve()
                .toBodilessEntity();
    }

}
