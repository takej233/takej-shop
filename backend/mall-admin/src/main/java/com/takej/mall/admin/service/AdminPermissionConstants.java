package com.takej.mall.admin.service;

/**
 * 后台权限码常量。
 * 权限码是角色与代码之间的契约，集中定义避免写串。
 */
public final class AdminPermissionConstants {

    /** 新增商品。 */
    public static final String PRODUCT_CREATE = "PRODUCT_CREATE";

    /** 修改商品。 */
    public static final String PRODUCT_UPDATE = "PRODUCT_UPDATE";

    /** 商品上下架。 */
    public static final String PRODUCT_STATUS = "PRODUCT_STATUS";

    /** 删除商品。 */
    public static final String PRODUCT_DELETE = "PRODUCT_DELETE";

    /** 调整库存。 */
    public static final String STOCK_ADJUST = "STOCK_ADJUST";

    /** 关闭订单。 */
    public static final String CLOSE_ORDER = "CLOSE_ORDER";

    /** 网关注入的管理员 ID 请求头。 */
    public static final String HEADER_ADMIN_ID = "X-Admin-Id";

    private AdminPermissionConstants() {
    }
}