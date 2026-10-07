import { request } from './http'
import type { UserProfile } from '@/types/api'

export interface UpdateProfilePayload {
  /** 昵称必填，最长 50 字符 */
  nickname: string
  avatar?: string | null
  /** 0 未知，1 男，2 女 */
  gender?: number | null
  phone?: string | null
}

/** 查询自己的资料。历史账号可能没有资料，会返回 404 */
export function getMyProfile() {
  return request<UserProfile>({ url: '/user/me', method: 'get' })
}

/** 修改自己的资料 */
export function updateMyProfile(payload: UpdateProfilePayload) {
  return request<null>({ url: '/user/me', method: 'put', data: payload })
}
