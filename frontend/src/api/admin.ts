import { adminRequest } from './http'
import type {
  AdminIdentity,
  AdminOperationLog,
  AdminOrderDetail,
  AdminOrderItem,
  AdminProductItem,
  AdminStockLogItem
} from '@/types/api'

/** 管理员登录请求参数 */
export interface AdminLoginPayload {
  username: string
  password: string
}

/** 新增商品请求参数 */
export interface CreateProductPayload {
  name: string
  subtitle?: string | null
  mainImage?: string | null
  price: number
  stock: number
  sort?: number | null
}

/**
 * 修改商品请求参数。
 * 故意不含 stock —— 库存只能通过 adjustStock 单独调整，
 * 避免编辑表单里带一个旧库存值把并发扣减的结果覆盖掉。
 */
export interface UpdateProductPayload {
  name: string
  subtitle?: string | null
  mainImage?: string | null
  price: number
  sort?: number | null
}

/** 调整库存请求参数。changeQuantity 是增量，不是目标值 */
export interface AdjustStockPayload {
  changeQuantity: number
  reason?: string | null
}

// ===== 认证 =====

export function adminLogin(username: string, password: string) {
  return adminRequest<{ adminId: number; username: string; token: string }>({
    url: '/admin/auth/login',
    method: 'post',
    data: { username, password }
  })
}

/** 退出登录。成功后后端会删掉 Redis 会话，Token 立即失效 */
export function adminLogout() {
  return adminRequest<null>({ url: '/admin/auth/logout', method: 'post' })
}

export function getAdminIdentity() {
  return adminRequest<AdminIdentity>({ url: '/admin/me', method: 'get' })
}

/** 当前管理员拥有的权限码列表 */
export function myPermissions() {
  return adminRequest<string[]>({ url: '/admin/me/permissions', method: 'get' })
}

// ===== 商品 =====

/** 后台商品列表。包含已下架和已逻辑删除的商品 */
export function adminListProducts() {
  return adminRequest<AdminProductItem[]>({ url: '/admin/products', method: 'get' })
}

export function adminCreateProduct(payload: CreateProductPayload) {
  return adminRequest<number>({ url: '/admin/products', method: 'post', data: payload })
}

export function adminUpdateProduct(productId: number, payload: UpdateProductPayload) {
  return adminRequest<null>({
    url: `/admin/products/${productId}`,
    method: 'put',
    data: payload
  })
}

export function adminUpdateProductStatus(productId: number, status: number) {
  return adminRequest<null>({
    url: `/admin/products/${productId}/status`,
    method: 'put',
    data: { status }
  })
}

export function adminDeleteProduct(productId: number) {
  return adminRequest<null>({ url: `/admin/products/${productId}`, method: 'delete' })
}

/** 调整库存。changeQuantity 是增量：正数加、负数减，不能为 0 */
export function adminAdjustStock(productId: number, payload: AdjustStockPayload) {
  return adminRequest<null>({
    url: `/admin/products/${productId}/adjust-stock`,
    method: 'post',
    data: payload
  })
}

/** 库存变动记录。source 为 null 时查全部：1人工调整 2下单扣减 3订单归还 */
export function adminListStockLogs(productId: number, source?: number | null) {
  return adminRequest<AdminStockLogItem[]>({
    url: `/admin/products/${productId}/stock-logs`,
    method: 'get',
    params: source == null ? {} : { source }
  })
}

// ===== 订单 =====

/** 全部用户的订单，最新在前 */
export function adminListOrders() {
  return adminRequest<AdminOrderItem[]>({ url: '/admin/orders', method: 'get' })
}

export function adminGetOrderDetail(orderId: number) {
  return adminRequest<AdminOrderDetail>({
    url: `/admin/orders/${orderId}`,
    method: 'get'
  })
}

/** 关闭待支付订单。成功后会归还库存 */
export function adminCloseOrder(orderId: number) {
  return adminRequest<null>({ url: `/admin/orders/${orderId}/close`, method: 'post' })
}

// ===== 日志 =====

/** 最近 100 条操作日志，最新在前 */
export function adminListOperationLogs() {
  return adminRequest<AdminOperationLog[]>({
    url: '/admin/operation-logs',
    method: 'get'
  })
}
