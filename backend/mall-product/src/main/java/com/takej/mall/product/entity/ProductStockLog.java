package com.takej.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 商品库存人工调整记录，对应 product_stock_log 表。
 * 这张表只追加，不修改、不删除，用于事后追溯库存变动。
 */
@TableName("product_stock_log")
public class ProductStockLog {

    /** 日志主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品 ID。 */
    private Long productId;

    /** 来源：1人工调整，2下单扣减，3订单归还。 */
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

    /** 创建时间。 */
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