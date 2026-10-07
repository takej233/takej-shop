import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import type { Result } from '@/types/api'

/** 商城用户 Token 的本地存储 key */
export const USER_TOKEN_KEY = 'takej_user_token'
/** 后台管理员 Token 的本地存储 key。两者必须分开，否则会互相覆盖导致全站 401 */
export const ADMIN_TOKEN_KEY = 'takej_admin_token'

/**
 * 业务错误。
 * message 直接来自后端（后端写的是人话），前端不再二次包装。
 * status 留给页面做分支，比如 409 要提示"刷新后重试"。
 */
export class ApiError extends Error {
  readonly status: number
  readonly code: number | null

  constructor(status: number, message: string, code: number | null = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

/** 两个实例共用同一份创建逻辑 */
function createInstance() {
  return axios.create({
    baseURL: '/api', // 配合 Vite proxy，不要写成完整域名
    timeout: 10000
  })
}

/** 商城前台实例，绑定用户 Token */
export const http = createInstance()
/** 后台管理实例，绑定管理员 Token */
export const adminHttp = createInstance()

/** 给实例装上"自动带 Token"的请求拦截器 */
function attachToken(instance: typeof http, tokenKey: string) {
  instance.interceptors.request.use((config) => {
    const token = localStorage.getItem(tokenKey)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 红线：绝不手传 X-User-Id / X-Admin-Id，这两个由网关注入。
    // 手传不但没用（网关会先清掉伪造值），排查问题时还会误导自己。
    return config
  })
}

attachToken(http, USER_TOKEN_KEY)
attachToken(adminHttp, ADMIN_TOKEN_KEY)

/**
 * 统一的响应错误处理。
 *
 * @param loginPath 401 后跳到哪个登录页
 * @param tokenKey  401 时清掉哪一套身份的 Token。
 *                  商城和管理员是两套独立登录体系，必须各清各的 ——
 *                  否则管理员 Token 过期会把商城用户也一起登出，反之亦然。
 */
function handleError(
  error: AxiosError<Result<unknown>>,
  loginPath: string,
  tokenKey: string
): never {
  const status = error.response?.status ?? 0
  // 网关自己返回的 401 没有响应体，所以这里必须能容忍 undefined
  const body = error.response?.data

    if (status === 401) {
    const isLoginPage = window.location.pathname.startsWith(loginPath)

    if (isLoginPage) {
      // 已经在登录页收到 401，说明是"账号密码错误"这类业务失败。
      // 不要跳转（本来就在登录页），把后端原始提示透传出去。
      throw new ApiError(401, body?.message || '用户名或密码错误', 401)
    }

    // 其他页面收到 401：身份失效，清掉当前这一套身份的登录态并跳登录页。
    localStorage.removeItem(tokenKey)
    window.location.href = loginPath
    throw new ApiError(401, '登录已失效，请重新登录', 401)
  }


  if (status === 403) {
    // 身份没问题，只是权限不够：留在原页面提示，绝不跳登录页。
    // 否则用户点一个没权限的按钮就被踢出去，体验会很糟。
    throw new ApiError(403, body?.message || '没有该操作的权限', 403)
  }

  if (status === 404) {
    throw new ApiError(404, body?.message || '资源不存在', 404)
  }

  if (status === 409) {
    // 库存不足、用户名已存在、订单状态不允许操作都走这里
    throw new ApiError(409, body?.message || '当前状态不允许该操作', 409)
  }

  if (status >= 500) {
    throw new ApiError(status, '服务异常，请稍后重试', status)
  }

  // 其余错误（400 等）：把后端的中文提示原样透传给调用方
  throw new ApiError(
    status,
    body?.message || '请求失败，请稍后重试',
    body?.code ?? null
  )
}

http.interceptors.response.use(
  (r) => r,
  (e) => handleError(e, '/login', USER_TOKEN_KEY)
)

adminHttp.interceptors.response.use(
  (r) => r,
  (e) => handleError(e, '/admin/login', ADMIN_TOKEN_KEY)
)

/**
 * 统一拆包。
 * 成功时后端是 Result<T>，这里直接给出 data，页面拿到就是纯业务数据。
 * 失败时后端用 HTTP 状态码表达，已经被拦截器转成 ApiError 抛出了。
 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<Result<T>>(config)
  return response.data.data
}

/** 后台版拆包 */
export async function adminRequest<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await adminHttp.request<Result<T>>(config)
  return response.data.data
}


/** 判断是否是 ApiError，便于页面按状态码分支处理 */
export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

/** 取错误的可展示文案，页面直接丢给 ElMessage 就行 */
export function errorMessage(error: unknown, fallback = '操作失败'): string {
  if (isApiError(error)) return error.message
  if (error instanceof Error) return error.message
  return fallback
}
