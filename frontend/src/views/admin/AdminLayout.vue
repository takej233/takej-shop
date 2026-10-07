<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

/** 当前高亮的菜单项 */
const activeMenu = computed(() => route.path)

const permissionCount = computed(() => adminStore.permissions.length)

async function handleLogout() {
  await adminStore.logout()
  ElMessage.success('已退出登录')
  await router.replace('/admin/login')
}

async function handleRefreshPermissions() {
  await adminStore.refreshPermissions()
  ElMessage.success(`权限已刷新，共 ${adminStore.permissions.length} 项`)
}

// 每次进入后台都重新拉一次权限，
// 因为前端这份是登录时缓存的，管理员权限被调整后不会自动同步。
onMounted(() => {
  adminStore.refreshPermissions()
})
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="logo">takej 后台</div>

      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/admin/products">商品管理</el-menu-item>
        <el-menu-item index="/admin/orders">订单管理</el-menu-item>
        <el-menu-item index="/admin/operation-logs">操作日志</el-menu-item>
      </el-menu>
    </aside>

    <div class="right">
      <header class="topbar">
        <div class="left">
          <span class="admin">管理员：{{ adminStore.username }}</span>
          <el-tag size="small" type="info">权限 {{ permissionCount }} 项</el-tag>
        </div>

        <div class="ops">
          <el-button link type="primary" @click="handleRefreshPermissions">
            刷新权限
          </el-button>
          <el-button link type="danger" @click="handleLogout">退出登录</el-button>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.sidebar {
  width: 200px;
  flex-shrink: 0;
  background: #22252f;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding-left: 20px;
  color: #fff;
}
.logo .mark {
  font-size: 20px;
  font-weight: 800;
  color: var(--tk-brand);
  letter-spacing: -0.5px;
}
.logo .text {
  font-size: 13px;
  color: #c9ccd4;
}

.menu {
  border-right: none;
  margin-top: 8px;
}

.right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin {
  font-size: 14px;
  color: #606266;
}

.ops {
  display: flex;
  align-items: center;
  gap: 8px;
}

.content {
  flex: 1;
  padding: 20px;
  overflow: auto;
}
</style>
