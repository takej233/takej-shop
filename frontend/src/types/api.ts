/** 后端所有业务接口的统一响应结构 */
export interface Result<T> {
  code: number
  message: string
  data: T
}

/** 商城用户登录响应 */
export interface LoginResponse {
  userId: number
  username: string
  token: string
}

/** 后台管理员登录响应 */
export interface AdminLoginResponse {
  adminId: number
  username: string
  token: string
}

/** 商品：status 0下架 1上架；deleted 0正常 1已删除 */
export interface Product {
  id: number
  name: string
  subtitle: string | null
  mainImage: string | null
  price: number
  stock: number
  status: number
  sort: number
  createTime: string
  updateTime: string
  deleted: number
}

/**
 * 购物车项。
 * 注意：后端只给 productId，没有商品名、图片、价格，
 * 展示时需要另外查商品补全。
 */
export interface CartItem {
  id: number
  userId: number
  productId: number
  quantity: number
  checked: number
  createTime: string
  updateTime: string
  deleted: number
}

/** 订单主表：status 含义见 constants.ts */
export interface OrderInfo {
  id: number
  orderNo: string
  userId: number
  totalAmount: number
  status: number
  expireTime: string
  createTime: string
  updateTime: string
  deleted: number
}

/** 订单明细：productName 和 price 是下单时的快照，不随商品改价而变 */
export interface OrderItem {
  id: number
  orderId: number
  productId: number
  productName: string
  price: number
  quantity: number
  subtotal: number
  createTime: string
}

/** 订单详情：后端把主表和明细包在一起返回 */
export interface OrderDetail {
  order: OrderInfo
  items: OrderItem[]
}

/** 用户资料：gender 0未知 1男 2女 */
export interface UserProfile {
  id: number
  userId: number
  nickname: string
  avatar: string | null
  gender: number
  phone: string | null
  createTime: string
  updateTime: string
  deleted: number
}

// ===================== 后台管理 =====================

/** 管理员身份 */
export interface AdminIdentity {
  adminId: number
  username: string
}

/** 后台商品列表项。包含已下架和已逻辑删除的商品 */
export interface AdminProductItem {
  id: number
  name: string
  /** 副标题。后端补全后才会有值，见后端修复说明 */
  subtitle: string | null
  /** 主图地址 */
  mainImage: string | null
  price: number
  stock: number
  status: number
  /** 排序值 */
  sort: number | null
  deleted: number
}

/** 后台订单列表项 */
export interface AdminOrderItem {
  id: number
  orderNo: string
  userId: number
  totalAmount: number
  status: number
  expireTime: string
  createTime: string
}

/** 后台订单明细行：名称和价格都是下单时的快照 */
export interface AdminOrderLine {
  id: number
  orderId: number
  productId: number
  productName: string
  price: number
  quantity: number
  subtotal: number
}

/** 后台订单详情 */
export interface AdminOrderDetail {
  order: AdminOrderItem
  items: AdminOrderLine[]
}

/** 库存变动记录 */
export interface AdminStockLogItem {
  id: number
  productId: number
  source: number
  changeQuantity: number
  stockBefore: number
  stockAfter: number
  reason: string | null
  /** 人工调整时有值，系统行为为 null */
  operatorId: number | null
  createTime: string
}

/** 管理员操作日志 */
export interface AdminOperationLog {
  id: number
  adminId: number
  operation: string
  targetType: string
  targetId: number
  detail: string
  createTime: string
}
