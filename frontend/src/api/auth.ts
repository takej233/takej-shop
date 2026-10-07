import { request } from './http'
import type { LoginResponse } from '@/types/api'

/** 用户登录：成功返回 { userId, username, token } */
export function login(username: string, password: string) {
  return request<LoginResponse>({
    url: '/auth/login', // baseURL 已经带了 /api，这里不重复写
    method: 'post',
    data: { username, password }
  })
}

/**
 * 注册：成功只返回新用户 ID，**不会自动登录**。
 * 调用方需要再走一次 login。
 * 用户名已存在时后端返回 409。
 */
export function register(username: string, password: string) {
  return request<number>({
    url: '/auth/register',
    method: 'post',
    data: { username, password }
  })
}
