package com.takej.mall.admin.client;

import com.takej.mall.admin.dto.AdminProductItem;
import com.takej.mall.common.BusinessException;
import com.takej.mall.common.ErrorCode;
import com.takej.mall.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import com.takej.mall.admin.dto.AdminCreateProductRequest;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientResponseException;
import com.takej.mall.admin.dto.AdminUpdateProductRequest;
import com.takej.mall.admin.dto.AdminAdjustStockRequest;
import java.util.HashMap;
import java.util.Map;
import com.takej.mall.admin.dto.AdminStockLogItem;
import com.takej.mall.admin.dto.AdminUpdateProductStatusRequest;


/**
 * 后台商品服务客户端。
 * 通过内部接口读取商品服务中的全部商品。
 */
@Service
public class ProductAdminClient {

    private final RestClient restClient;

    @Value("${takej.product.base-url}")
    private String productBaseUrl;

    public ProductAdminClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * 查询全部商品，包含下架和逻辑删除商品。
     *
     * @return 后台商品列表
     */
    public List<AdminProductItem> listAllProducts() {
        Result<List<AdminProductItem>> result =
                restClient.get()
                        .uri(productBaseUrl
                                + "/internal/products/all")
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {
                        });

        // 商品服务没有返回统一结果时，视为内部服务异常。
        if (result == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        // 商品列表为空时返回空集合，避免后台 Controller 出现空指针。
        if (result.getData() == null) {
            return Collections.emptyList();
        }

        return result.getData();
    }


    /**
     * 通过商品服务内部接口创建商品。
     *
     * @param request 后台新增商品参数
     * @return 新商品 ID
     */
    public Long createProduct(
            AdminCreateProductRequest request) {

        try {
            Result<Long> result = restClient.post()
                    .uri(productBaseUrl + "/internal/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            // 商品服务没有返回统一结果或商品 ID 时，视为内部异常。
            if (result == null || result.getData() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }

            return result.getData();

        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            // 商品参数校验失败，转成后台服务自己的 400。
            if (status == 400) {
                throw new BusinessException(
                        400,
                        "商品参数校验失败"
                );
            }

            // 其他商品服务 HTTP 错误统一视为内部调用失败。
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR
            );
        }
    }


    /**
     * 通过商品服务内部接口修改商品基本信息。
     * 该接口不会修改商品库存。
     *
     * @param productId 商品 ID
     * @param request   后台修改商品参数
     */
    public void updateProduct(
            Long productId,
            AdminUpdateProductRequest request) {

        try {
            Result<Void> result = restClient.put()
                    .uri(productBaseUrl
                                    + "/internal/products/{productId}",
                            productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (result == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }

        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            // 商品不存在或已被逻辑删除。
            if (status == 404) {
                throw new BusinessException(
                        404,
                        "商品不存在或已删除"
                );
            }

            // 商品参数校验失败。
            if (status == 400) {
                throw new BusinessException(
                        400,
                        "商品参数校验失败"
                );
            }

            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }


    /**
     * 通过商品服务内部接口修改商品上下架状态。
     *
     * @param productId 商品 ID
     * @param request   状态参数
     */
    public void updateProductStatus(
            Long productId,
            AdminUpdateProductStatusRequest request) {

        try {
            Result<Void> result = restClient.put()
                    .uri(productBaseUrl
                                    + "/internal/products/{productId}/status",
                            productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (result == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }

        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            // 商品不存在或已被逻辑删除。
            if (status == 404) {
                throw new BusinessException(
                        404,
                        "商品不存在或已删除"
                );
            }

            // 状态取值不合法。
            if (status == 400) {
                throw new BusinessException(
                        400,
                        "商品状态参数不合法"
                );
            }

            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }



    /**
     * 通过商品服务内部接口逻辑删除商品。
     *
     * @param productId 商品 ID
     */
    public void deleteProduct(Long productId) {
        try {
            Result<Void> result = restClient.delete()
                    .uri(productBaseUrl
                                    + "/internal/products/{productId}",
                            productId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (result == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }

        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            // 商品不存在，或已经被删除过。
            if (status == 404) {
                throw new BusinessException(
                        404,
                        "商品不存在或已删除"
                );
            }

            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }






    /**
     * 通过商品服务内部接口人工调整库存。
     * operatorId 由后台服务从网关注入的身份取出，不使用前端提交的值。
     *
     * @param productId  商品 ID
     * @param operatorId 操作管理员 ID
     * @param request    调整参数
     */
    public void adjustStock(
            Long productId,
            Long operatorId,
            AdminAdjustStockRequest request) {

        // 这里用 HashMap 而不是 Map.of：
        // Map.of 不接受 null 值，而 reason 是可以不填的。
        Map<String, Object> body = new HashMap<>();
        body.put("changeQuantity", request.getChangeQuantity());
        body.put("reason", request.getReason());
        body.put("operatorId", operatorId);

        try {
            Result<Void> result = restClient.post()
                    .uri(productBaseUrl
                                    + "/internal/products/{productId}/adjust-stock",
                            productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (result == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }

        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            // 商品不存在或已被逻辑删除。
            if (status == 404) {
                throw new BusinessException(
                        404,
                        "商品不存在或已删除"
                );
            }

            // 调整后库存会变成负数。
            if (status == 409) {
                throw new BusinessException(
                        409,
                        "调整后库存不能为负数"
                );
            }

            // 变动数量、操作人等参数不合法。
            if (status == 400) {
                throw new BusinessException(
                        400,
                        "库存调整参数不合法"
                );
            }

            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 查询某个商品的库存变动记录。
     *
     * @param productId 商品 ID
     * @param source    来源筛选，为 null 时查询全部
     * @return 库存变动记录，最新在前
     */
    public List<AdminStockLogItem> listStockLogs(
            Long productId,
            Integer source) {

        String uri = productBaseUrl
                + "/internal/products/" + productId + "/stock-logs";

        // source 是 Integer，拼接进 URL 不存在注入风险。
        if (source != null) {
            uri = uri + "?source=" + source;
        }

        Result<List<AdminStockLogItem>> result =
                restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {
                        });

        if (result == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        if (result.getData() == null) {
            return Collections.emptyList();
        }

        return result.getData();
    }


}