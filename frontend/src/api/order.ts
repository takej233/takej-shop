import { request } from './http'
import type { OrderDetail, OrderInfo } from '@/types/api'

export interface CreateOrderPayload {
  items: { productId: number; quantity: number }[]
}

/**
 * 生成下单幂等号。
 * 每次点击下单时生成一个新的，随请求一起发给后端。
 * 后端用它保证"同一请求只创建一笔订单"：
 * 重复提交会返回第一次创建的订单 ID，不会重复扣库存。
 */
export function newRequestId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  // 老浏览器兜底：时间戳 + 随机串，足够避免碰撞
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

/** 直接下单（用于「立即购买」，不走购物车） */
export function createOrder(payload: CreateOrderPayload, requestId?: string) {
  return request<number>({
    url: '/orders',
    method: 'post',
    data: { ...payload, requestId }
  })
}

/** 购物车结算：把已勾选的商品下单，成功后后端会清空已勾选项 */
export function createOrderFromCart(requestId?: string) {
  return request<number>({
    url: '/orders/from-cart',
    method: 'post',
    params: requestId ? { requestId } : {}
  })
}

/** 我的订单列表（只有主表，没有商品明细） */
export function listOrders() {
  return request<OrderInfo[]>({ url: '/orders', method: 'get' })
}

/** 订单详情：包含下单时的商品名称与价格快照 */
export function getOrderDetail(orderId: number) {
  return request<OrderDetail>({ url: `/orders/${orderId}`, method: 'get' })
}

/** 支付。只有待支付订单能支付；订单已超时会返回 400 */
export function payOrder(orderId: number) {
  return request<null>({ url: `/orders/${orderId}/pay`, method: 'post' })
}

/** 取消。只有待支付订单能取消 */
export function cancelOrder(orderId: number) {
  return request<null>({ url: `/orders/${orderId}/cancel`, method: 'put' })
}

/** 确认收货。只有已支付订单能确认 */
export function completeOrder(orderId: number) {
  return request<null>({ url: `/orders/${orderId}/complete`, method: 'put' })
}
