package com.takej.mall.admin.service;

/**
 * 后台操作日志中使用的常量。
 * 集中定义避免在各处写裸字符串写错，也方便后续统一查阅。
 */
public final class AdminOperationConstants {

    /** 删除商品。 */
    public static final String DELETE_PRODUCT = "DELETE_PRODUCT";

    /** 修改商品上下架状态。 */
    public static final String UPDATE_PRODUCT_STATUS =
            "UPDATE_PRODUCT_STATUS";

    /** 新增商品。 */
    public static final String CREATE_PRODUCT = "CREATE_PRODUCT";

    /** 修改商品基本信息。 */
    public static final String UPDATE_PRODUCT = "UPDATE_PRODUCT";

    /** 人工调整库存。 */
    public static final String ADJUST_STOCK = "ADJUST_STOCK";

    /** 关闭订单。 */
    public static final String CLOSE_ORDER = "CLOSE_ORDER";

    /** 目标对象：商品。 */
    public static final String TARGET_PRODUCT = "PRODUCT";

    /** 目标对象：订单。 */
    public static final String TARGET_ORDER = "ORDER";

    private AdminOperationConstants() {
    }
}