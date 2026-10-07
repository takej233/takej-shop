package com.takej.mall.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * 后台商品列表项。
 * 只暴露后台列表当前需要的字段。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminProductItem {

    /** 商品 ID。 */
    private Long id;

    /** 商品名称。 */
    private String name;

    /**
     * 商品副标题。
     * 商品服务 /internal/products/all 返回的本来就是完整商品，
     * 这里必须保留该字段，否则后台编辑表单回填不到值，
     * 保存时又走全量更新，会把原有的副标题清空。
     */
    private String subtitle;

    /** 商品主图地址。 */
    private String mainImage;

    /** 商品价格。 */
    private BigDecimal price;

    /** 当前库存。 */
    private Integer stock;

    /** 状态：0 下架，1 上架。 */
    private Integer status;

    /** 逻辑删除：0 正常，1 删除。 */
    private Integer deleted;

    /** 排序值，越大越靠前。 */
    private Integer sort;

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

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getMainImage() {
        return mainImage;
    }

    public void setMainImage(String mainImage) {
        this.mainImage = mainImage;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}