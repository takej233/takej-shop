package com.takej.mall.admin.client;

import com.takej.mall.admin.dto.AdminOrderItem;
import com.takej.mall.common.BusinessException;
import com.takej.mall.common.ErrorCode;
import com.takej.mall.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientResponseException;
import java.util.HashMap;
import java.util.Map;
import com.takej.mall.admin.dto.AdminOrderDetail;


/**
 * 后台订单服务客户端。
 * 通过内部接口读取订单服务中的全部订单。
 */
@Service
public class OrderAdminClient {

    private final RestClient restClient;

    @Value("${takej.order.base-url}")
    private String orderBaseUrl;

    public OrderAdminClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * 查询全部订单，最新在前。
     *
     * @return 后台订单列表
     */
    public List<AdminOrderItem> listAllOrders() {
        Result<List<AdminOrderItem>> result =
                restClient.get()
                        .uri(orderBaseUrl + "/internal/orders/all")
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {
                        });

        if (result == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        // 没有订单时返回空集合，避免后台列表出现空指针。
        if (result.getData() == null) {
            return Collections.emptyList();
        }

        return result.getData();
    }


    /**
     * 通过订单服务内部接口关闭一笔待支付订单。
     * operatorId 由后台服务从网关注入的身份取出，不使用前端提交的值。
     *
     * @param orderId    订单 ID
     * @param operatorId 操作管理员 ID
     */
    public void closeOrder(Long orderId, Long operatorId) {

        // 订单服务内部接口只要求一个 operatorId。
        Map<String, Object> body = new HashMap<>();
        body.put("operatorId", operatorId);

        try {
            Result<Void> result = restClient.post()
                    .uri(orderBaseUrl
                                    + "/internal/orders/{orderId}/close",
                            orderId)
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

            // 订单不存在，或已经是终态、不允许再关闭。
            if (status == 409) {
                throw new BusinessException(
                        409,
                        "订单当前状态不允许关闭"
                );
            }

            if (status == 400) {
                throw new BusinessException(
                        400,
                        "订单关闭参数不合法"
                );
            }

            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }


    /**
     * 查询一笔订单的详情，包含商品明细。
     *
     * @param orderId 订单 ID
     * @return 订单详情
     */
    public AdminOrderDetail getOrderDetail(Long orderId) {
        try {
            Result<AdminOrderDetail> result = restClient.get()
                    .uri(orderBaseUrl
                                    + "/internal/orders/{orderId}",
                            orderId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (result == null || result.getData() == null) {
                throw new BusinessException(
                        ErrorCode.INTERNAL_ERROR
                );
            }

            return result.getData();

        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            // 订单不存在或已逻辑删除。
            if (status == 404) {
                throw new BusinessException(404, "订单不存在");
            }

            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }


    /**
     * 统计某商品还有多少笔待支付订单。
     *
     * @param productId 商品 ID
     * @return 待支付订单数量
     */
    public int countPendingOrders(Long productId) {
        Result<Integer> result = restClient.get()
                .uri(orderBaseUrl
                                + "/internal/orders/pending-count?productId={productId}",
                        productId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (result == null || result.getData() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        return result.getData();
    }


}