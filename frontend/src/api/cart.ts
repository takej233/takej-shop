import { request } from './http'
import type { CartItem } from '@/types/api'

/** 加入购物车。重复加入同一商品，后端会累加数量并把 checked 置回 1 */
export function addItem(productId: number, quantity: number) {
  return request<null>({
    url: '/cart/items',
    method: 'post',
    data: { productId, quantity }
  })
}

/** 当前用户购物车列表 */
export function listItems() {
  return request<CartItem[]>({ url: '/cart/items', method: 'get' })
}

/** 改数量。quantity 必须 >= 1，传 0 或负数后端返回 400 */
export function updateQuantity(productId: number, quantity: number) {
  return request<null>({
    url: `/cart/items/${productId}`,
    method: 'put',
    data: { quantity }
  })
}

/** 勾选/取消勾选。checked 只能是 0 或 1 */
export function updateChecked(productId: number, checked: number) {
  return request<null>({
    url: `/cart/items/${productId}/checked`,
    method: 'put',
    data: { checked }
  })
}

/** 删除单项 */
export function deleteItem(productId: number) {
  return request<null>({ url: `/cart/items/${productId}`, method: 'delete' })
}

/** 删除所有已勾选 */
export function deleteSelected() {
  return request<null>({ url: '/cart/items/selected', method: 'delete' })
}
