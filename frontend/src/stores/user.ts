import { defineStore } from 'pinia'
import { ref } from 'vue'
import { USER_TOKEN_KEY } from '@/api/http'
import { login as loginApi } from '@/api/auth'

/** 用户信息也存本地，否则刷新页面后昵称就没了 */
const USER_INFO_KEY = 'takej_user_info'

interface StoredUser {
  userId: number
  username: string
}

function readStoredUser(): StoredUser | null {
  const raw = localStorage.getItem(USER_INFO_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as StoredUser
  } catch {
    // 本地数据被改坏时不要崩，当作没存过
    localStorage.removeItem(USER_INFO_KEY)
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const stored = readStoredUser()

  const token = ref<string | null>(localStorage.getItem(USER_TOKEN_KEY))
  const userId = ref<number | null>(stored?.userId ?? null)
  const username = ref<string>(stored?.username ?? '')

  async function login(name: string, password: string) {
    const data = await loginApi(name, password)

    token.value = data.token
    userId.value = data.userId
    username.value = data.username

    localStorage.setItem(USER_TOKEN_KEY, data.token)
    localStorage.setItem(USER_INFO_KEY, JSON.stringify({
      userId: data.userId,
      username: data.username
    }))
  }

  function logout() {
    // 普通用户后端没有退出接口，只能清本地。
    // 要清楚这意味着：服务端那个 Token 在 20 小时内仍然有效。
    token.value = null
    userId.value = null
    username.value = ''
    localStorage.removeItem(USER_TOKEN_KEY)
    localStorage.removeItem(USER_INFO_KEY)
  }

  return { token, userId, username, login, logout }
})
