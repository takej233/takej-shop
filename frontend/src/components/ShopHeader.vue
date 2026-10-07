<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()

const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')

/** 搜索：只是前端过滤，把关键字放进地址栏，商品页自己读 */
function handleSearch() {
  router.push({
    path: '/products',
    query: keyword.value ? { keyword: keyword.value } : {}
  })
}

async function handleLogout() {
  userStore.logout()
  cartStore.reset()
  ElMessage.success('已退出登录')
  await router.replace('/login')
}

onMounted(() => {
  // 没登录就别去请求，省得 401 打乱页面
  if (userStore.token) cartStore.refresh()
})
</script>

<template>
  <header class="tk-header">
    <!-- 顶部细条：用户区 -->
    <div class="topbar">
      <div class="wrap">
        <span v-if="userStore.username" class="hello">
          你好，{{ userStore.username }}
        </span>
        <div class="top-links">
          <span class="link" @click="router.push('/orders')">我的订单</span>
          <span class="divider">|</span>
          <span class="link" @click="router.push('/profile')">个人资料</span>
          <span class="divider">|</span>
          <span class="link danger" @click="handleLogout">退出</span>
        </div>
      </div>
    </div>

    <!-- 主导航：Logo + 菜单 + 搜索 + 购物车 -->
    <div class="mainbar">
      <div class="wrap">
        <div class="logo" @click="router.push('/products')">
          <span class="logo-mark">takej</span>
          <span class="logo-text">商城</span>
        </div>

        <nav class="nav">
          <span
            class="nav-item"
            :class="{ active: route.path.startsWith('/products') }"
            @click="router.push('/products')"
          >
            全部商品
          </span>
          <span
            class="nav-item"
            :class="{ active: route.path === '/cart' }"
            @click="router.push('/cart')"
          >
            购物车
          </span>
          <span
            class="nav-item"
            :class="{ active: route.path.startsWith('/orders') }"
            @click="router.push('/orders')"
          >
            我的订单
          </span>
        </nav>

        <div class="search">
          <el-input
            v-model="keyword"
            class="search-input"
            size="large"
            placeholder="搜索商品名称"
            clearable
            @keyup.enter="handleSearch"
          />
          <el-button class="search-btn" size="large" @click="handleSearch">
            搜索
          </el-button>
        </div>

        <div class="cart" @click="router.push('/cart')">
          <svg class="cart-icon" viewBox="0 0 24 24" width="18" height="18">
            <path
              fill="currentColor"
              d="M7 18a2 2 0 1 0 0 4 2 2 0 0 0 0-4zm10 0a2 2 0 1 0 0 4 2 2 0 0 0 0-4zM1 2h3.5l1 3H21l-2.6 7.4a2 2 0 0 1-1.9 1.35H7.2a2 2 0 0 1-1.9-1.35L2.2 4.5A1 1 0 0 1 1.2 3.8 1 1 0 0 1 1 2zm6.7 7h10.9l1.9-5H6.2l1.5 5z"
            />
          </svg>
          <span>购物车</span>
          <span v-if="cartStore.count > 0" class="badge">
            {{ cartStore.count > 99 ? '99+' : cartStore.count }}
          </span>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.tk-header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.wrap {
  width: 100%;
  max-width: var(--tk-container);
  margin: 0 auto;
  padding: 0 16px;
  display: flex;
  align-items: center;
}

/* ---------- 顶部细条 ---------- */
.topbar {
  height: 36px;
  background: #f7f7f7;
  border-bottom: 1px solid #eee;
  font-size: 12px;
  color: #666;
}
.topbar .wrap {
  justify-content: space-between;
  height: 36px;
}
.hello {
  color: #666;
}
.top-links {
  display: flex;
  align-items: center;
  gap: 8px;
}
.link {
  cursor: pointer;
}
.link:hover {
  color: var(--tk-brand);
}
.link.danger:hover {
  color: var(--tk-price);
}
.divider {
  color: #ddd;
}

/* ---------- 主导航 ---------- */
.mainbar {
  height: var(--tk-header-h);
}
.mainbar .wrap {
  height: var(--tk-header-h);
  gap: 24px;
}

.logo {
  display: flex;
  align-items: baseline;
  gap: 4px;
  cursor: pointer;
  user-select: none;
}
.logo-mark {
  font-size: 24px;
  font-weight: 800;
  color: var(--tk-brand);
  letter-spacing: -0.5px;
}
.logo-text {
  font-size: 14px;
  font-weight: 600;
  color: #666;
}

.nav {
  display: flex;
  align-items: center;
  gap: 4px;
}
.nav-item {
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 14px;
  color: #555;
  cursor: pointer;
  white-space: nowrap;
}
.nav-item:hover {
  color: var(--tk-brand);
  background: #fff7f2;
}
.nav-item.active {
  color: var(--tk-brand);
  font-weight: 600;
}

.search {
  flex: 1;
  max-width: 480px;
  display: flex;
  align-items: center;
}

/* 输入框只保留左边圆角，右边缘和按钮拼成一体 */
.search-input {
  flex: 1;
}
.search-input :deep(.el-input__wrapper) {
  border-radius: 6px 0 0 6px;
  box-shadow: 0 0 0 1px var(--tk-brand) inset;
}

/* 搜索按钮：独立的大按钮，40px 高、96px 宽，不再被输入框挤扁 */
.search-btn {
  width: 96px;
  height: 40px;
  flex-shrink: 0;
  border: none;
  border-radius: 0 6px 6px 0;
  background: var(--tk-brand);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
}
.search-btn:hover,
.search-btn:focus {
  background: var(--tk-brand-hover);
  color: #fff;
}

.cart {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 16px;
  border: 1px solid var(--tk-brand);
  border-radius: 17px;
  color: var(--tk-brand);
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}
.cart:hover {
  background: var(--tk-brand);
  color: #fff;
}
.cart-icon {
  display: block;
}
.badge {
  position: absolute;
  top: -6px;
  right: -4px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--tk-price);
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

/* 窄屏收缩：先砍搜索框，再砍导航 */
@media (max-width: 860px) {
  .search {
    display: none;
  }
}
@media (max-width: 620px) {
  .nav {
    display: none;
  }
  .logo-text {
    display: none;
  }
}
</style>
