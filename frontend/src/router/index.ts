import { createRouter, createWebHistory } from 'vue-router'
import { USER_TOKEN_KEY, ADMIN_TOKEN_KEY } from '@/api/http'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/products' },

    // ===== 商城前台 =====
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/shop/LoginView.vue')
    },
    {
      // 注册页是公开页：不加 requiresAuth，守卫会直接放行
      path: '/register',
      name: 'register',
      component: () => import('@/views/shop/RegisterView.vue')
    },
    {
      path: '/products',
      name: 'products',
      meta: { requiresAuth: true },
      component: () => import('@/views/shop/ProductListView.vue')
    },
    {
      path: '/products/:id',
      name: 'product-detail',
      meta: { requiresAuth: true },
      component: () => import('@/views/shop/ProductDetailView.vue')
    },
    {
      path: '/cart',
      name: 'cart',
      meta: { requiresAuth: true },
      component: () => import('@/views/shop/CartView.vue')
    },
    {
      path: '/orders',
      name: 'orders',
      meta: { requiresAuth: true },
      component: () => import('@/views/shop/OrderListView.vue')
    },
    {
      path: '/orders/:id',
      name: 'order-detail',
      meta: { requiresAuth: true },
      component: () => import('@/views/shop/OrderDetailView.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      meta: { requiresAuth: true },
      component: () => import('@/views/shop/ProfileView.vue')
    },

    // ===== 后台管理 =====
    {
      // 管理员登录页单独放，不套后台布局
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('@/views/admin/AdminLoginView.vue')
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      redirect: '/admin/products',
      children: [
        {
          path: 'products',
          name: 'admin-products',
          component: () => import('@/views/admin/ProductManageView.vue')
        },
        {
          path: 'orders',
          name: 'admin-orders',
          component: () => import('@/views/admin/OrderManageView.vue')
        },
        {
          path: 'operation-logs',
          name: 'admin-operation-logs',
          component: () => import('@/views/admin/OperationLogView.vue')
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  // 后台路由用管理员 Token 判断，和商城完全分开
  const isAdminRoute = to.path.startsWith('/admin') && to.path !== '/admin/login'
  if (isAdminRoute) {
    if (!localStorage.getItem(ADMIN_TOKEN_KEY)) {
      return { path: '/admin/login', query: { redirect: to.fullPath } }
    }
    return true
  }

  const needLogin = to.meta.requiresAuth === true
  if (needLogin && !localStorage.getItem(USER_TOKEN_KEY)) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
