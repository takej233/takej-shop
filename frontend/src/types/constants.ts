/**
 * 订单状态。
 * 按后端 OrderStatus 的真实数值整理 —— 后端枚举里 EXPIRED 和 COMPLETED
 * 的书写顺序是反的，别照着枚举顺序抄。
 */
export const ORDER_STATUS = {
  PENDING_PAYMENT: 0, // 待支付
  PAID: 1,            // 已支付
  CANCELLED: 2,       // 已取消
  COMPLETED: 3,       // 已完成
  EXPIRED: 4          // 已过期（超过 30 分钟被定时任务关闭）
} as const

/** 状态值 → 展示文案 */
export const ORDER_STATUS_TEXT: Record<number, string> = {
  0: '待支付',
  1: '已支付',
  2: '已取消',
  3: '已完成',
  4: '已过期'
}

/** 商品状态 */
export const PRODUCT_STATUS = {
  OFF: 0,   // 下架
  ON: 1     // 上架
} as const

/** 后台操作类型：展示时用具名常量，别硬编码中文 */
export const OPERATION_TEXT: Record<string, string> = {
  CREATE_PRODUCT: '新增商品',
  UPDATE_PRODUCT: '修改商品',
  UPDATE_PRODUCT_STATUS: '上下架',
  DELETE_PRODUCT: '删除商品',
  ADJUST_STOCK: '调整库存',
  CLOSE_ORDER: '关闭订单'
}

/** 库存变动来源：1人工调整，2下单扣减，3订单归还 */
export const STOCK_SOURCE_TEXT: Record<number, string> = {
  1: '人工调整',
  2: '下单扣减',
  3: '订单归还'
}

/** 后台权限码。前端隐藏按钮只是体验优化，真正的拦截在后端切面 */
export const PERMISSION = {
  PRODUCT_CREATE: 'PRODUCT_CREATE',
  PRODUCT_UPDATE: 'PRODUCT_UPDATE',
  PRODUCT_STATUS: 'PRODUCT_STATUS',
  PRODUCT_DELETE: 'PRODUCT_DELETE',
  STOCK_ADJUST: 'STOCK_ADJUST',
  CLOSE_ORDER: 'CLOSE_ORDER'
} as const
