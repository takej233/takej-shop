import { defineStore } from 'pinia'
import { ref } from 'vue'
import { listItems } from '@/api/cart'

/**
 * 只存一个「购物车里有多少种商品」，给顶栏的角标用。
 * 这里不缓存商品明细，明细仍然由购物车页自己拉，
 * 避免两处数据不一致。
 */
export const useCartStore = defineStore('cart', () => {
  const count = ref(0)

  /** 拉一次角标数量。失败静默：角标不准不影响用户操作 */
  async function refresh() {
    try {
      const items = await listItems()
      count.value = items.length
    } catch {
      // 未登录或网络异常时保持原值，不弹错误提示
    }
  }

  function reset() {
    count.value = 0
  }

  return { count, refresh, reset }
})
