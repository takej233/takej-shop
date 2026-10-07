package com.takej.mall.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 后台订单列表项。
 * 只暴露后台列表当前需要的字段。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminOrderItem {

    /** 订单 ID。 */
    private Long id;

    /** 订单编号。 */
    private String orderNo;

    /** 下单用户 ID。 */
    private Long userId;

    /** 订单总金额。 */
    private BigDecimal totalAmount;

    /** 状态：0待支付，1已支付，2已取消，3已完成，4已过期。 */
    private Integer status;

    /** 支付截止时间。 */
    private LocalDateTime expireTime;

    /** 创建时间。 */
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}