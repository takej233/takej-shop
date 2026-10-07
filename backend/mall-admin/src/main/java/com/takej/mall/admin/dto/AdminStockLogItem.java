package com.takej.mall.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * 后台库存调整记录项。
 * 只暴露后台展示需要的字段。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminStockLogItem {

    /** 日志 ID。 */
    private Long id;

    /** 商品 ID。 */
    private Long productId;

    /** 变动来源：1人工调整，2下单扣减，3订单归还。 */
    private Integer source;

    /** 本次变动数量，正数为增加，负数为减少。 */
    private Integer changeQuantity;

    /** 变动前库存。 */
    private Integer stockBefore;

    /** 变动后库存。 */
    private Integer stockAfter;

    /** 调整原因。 */
    private String reason;

    /** 操作管理员 ID。 */
    private Long operatorId;

    /** 调整时间。 */
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getSource() {
        return source;
    }

    public void setSource(Integer source) {
        this.source = source;
    }

    public Integer getChangeQuantity() {
        return changeQuantity;
    }

    public void setChangeQuantity(Integer changeQuantity) {
        this.changeQuantity = changeQuantity;
    }

    public Integer getStockBefore() {
        return stockBefore;
    }

    public void setStockBefore(Integer stockBefore) {
        this.stockBefore = stockBefore;
    }

    public Integer getStockAfter() {
        return stockAfter;
    }

    public void setStockAfter(Integer stockAfter) {
        this.stockAfter = stockAfter;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}