package com.takej.mall.order.dto;

import java.math.BigDecimal;

/**
 * 商品服务返回给订单服务的下单商品信息。
 */
public class ProductSnapshot {

    /** 商品 ID */
    private Long id;

    /** 商品名称 */
    private String name;

    /** 当前售价 */
    private BigDecimal price;

    /** 商品状态：0下架，1上架 */
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}