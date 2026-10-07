import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ADMIN_TOKEN_KEY } from '@/api/http'
import {
  adminLogin as adminLoginApi,
  adminLogout as adminLogoutApi,
  myPermissions
} from '@/api/admin'

const ADMIN_INFO_KEY = 'takej_admin_info'

interface StoredAdmin {
  adminId: number
  username: string
}

function readStoredAdmin(): StoredAdmin | null {
  const raw = localStorage.getItem(ADMIN_INFO_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as StoredAdmin
  } catch {
    localStorage.removeItem(ADMIN_INFO_KEY)
    return null
  }
}

export const useAdminStore = defineStore('admin', () => {
  const stored = readStoredAdmin()

  const token = ref<string | null>(localStorage.getItem(ADMIN_TOKEN_KEY))
  const adminId = ref<number | null>(stored?.adminId ?? null)
  const username = ref<string>(stored?.username ?? '')
  /** 登录时拉取的权限码快照 */
  const permissions = ref<string[]>([])

  async function login(name: string, password: string) {
    const data = await adminLoginApi(name, password)

    token.value = data.token
    adminId.value = data.adminId
    username.value = data.username

    localStorage.setItem(ADMIN_TOKEN_KEY, data.token)
    localStorage.setItem(ADMIN_INFO_KEY, JSON.stringify({
      adminId: data.adminId,
      username: data.username
    }))

    // 登录后立刻拉一次权限，供按钮显示与否判断
    await refreshPermissions()
  }

  /**
   * 重新拉取权限码。
   * 后端每次请求都查库，权限被调整后不用重新登录就生效；
   * 但前端这份是缓存的，所以要在进入后台或手动刷新时重拉。
   */
  async function refreshPermissions() {
    try {
      permissions.value = await myPermissions()
    } catch {
      permissions.value = []
    }
  }

  async function logout() {
    try {
      // 这一步很关键：通知后端删掉 Redis 会话，让 Token 立即失效
      await adminLogoutApi()
    } catch {
      // 后端登出失败（比如会话已过期）也必须清本地，不能把用户卡在后台
    } finally {
      token.value = null
      adminId.value = null
      username.value = ''
      permissions.value = []
      localStorage.removeItem(ADMIN_TOKEN_KEY)
      localStorage.removeItem(ADMIN_INFO_KEY)
    }
  }

  function hasPermission(code: string): boolean {
    return permissions.value.includes(code)
  }

  return {
    token,
    adminId,
    username,
    permissions,
    login,
    logout,
    refreshPermissions,
    hasPermission
  }
})
