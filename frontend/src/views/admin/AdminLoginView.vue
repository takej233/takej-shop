<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAdminStore } from '@/stores/admin'
import { errorMessage } from '@/api/http'

const router = useRouter()
const route = useRoute()
const adminStore = useAdminStore()

const form = reactive({
  username: '',
  password: ''
})

const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入管理员用户名和密码')
    return
  }

  loading.value = true
  try {
    await adminStore.login(form.username, form.password)
    ElMessage.success('登录成功')

    const redirect = route.query.redirect
    await router.replace(typeof redirect === 'string' ? redirect : '/admin/products')
  } catch (error) {
    // 后端对「用户名不存在」和「密码错误」返回同一句话，这是有意的安全设计
    ElMessage.error(errorMessage(error, '登录失败'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <h2 class="title">takej 商城 · 后台管理</h2>
      </template>

      <el-form label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="管理员账号">
          <el-input v-model="form.username" placeholder="请输入管理员账号" clearable />
        </el-form-item>

        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-button
          class="tk-btn-main submit"
          size="large"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>

      <el-divider />

      <div class="tip">
        管理员账号由后端启动时初始化，本页面没有注册入口。
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  /* 和前台同一套品牌色渐变，只是角度不同，看起来是"同一个产品" */
  background: linear-gradient(150deg, #ff7a2f 0%, #ff5000 45%, #ff2b00 100%);
}

.login-card {
  width: 420px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.16);
}

.title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.submit {
  width: 100%;
}

.tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
