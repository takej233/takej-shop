package com.takej.mall.product.enums;

/**
 * 库存变动来源，对应 product_stock_log.source 字段。
 */
public final class ProductStockSource {

    /** 管理员人工调整。 */
    public static final int MANUAL_ADJUST = 1;

    /** 下单扣减库存。 */
    public static final int ORDER_DECREASE = 2;

    /** 订单取消、关闭或过期归还库存。 */
    public static final int ORDER_RESTORE = 3;

    private ProductStockSource() {
    }
}