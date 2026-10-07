import { request } from './http'
import type { Product } from '@/types/api'

/** 上架商品列表。后端已按 sort 倒序、id 倒序返回，前端直接展示即可 */
export function listProducts() {
  return request<Product[]>({ url: '/products', method: 'get' })
}

/** 商品详情。商品一旦下架或删除，这个接口返回 404 */
export function getProduct(id: number) {
  return request<Product>({ url: `/products/${id}`, method: 'get' })
}

/**
 * 批量查询商品，用于购物车等场景一次性补全商品信息。
 * 后端只返回上架且未删除的商品：请求里查不到的 ID 会被静默忽略，
 * 调用方按返回结果里有没有对应 ID 来判断商品是否仍可购买。
 */
export function listProductsByIds(ids: number[]) {
  return request<Product[]>({
    url: '/products/batch',
    method: 'get',
    params: { ids: ids.join(',') }
  })
}
