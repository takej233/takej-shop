<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { errorMessage } from '@/api/http'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive({
  // 从注册页跳过来时会带上用户名，预填一下省得用户再敲一遍
  username: typeof route.query.username === 'string' ? route.query.username : '',
  password: ''
})

const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')

    // 带着登录前的目标地址回来，没有就回首页
    const redirect = route.query.redirect
    await router.replace(typeof redirect === 'string' ? redirect : '/products')

  } catch (error) {
    // 后端的中文提示直接透传，前端不再包一层自己的文案
    ElMessage.error(errorMessage(error, '登录失败'))
  } finally {
    loading.value = false
  }
}

function goRegister() {
  router.push('/register')
}
</script>

<template>
  <div class="auth-page">
    <!-- 左：品牌展示区（背景图 + 品牌色遮罩） -->
    <aside class="side">
      <div class="side-inner">
        <div class="brand">
          <span class="brand-mark">takej</span>
          <span class="brand-text">商城</span>
        </div>
        <h2 class="slogan">好物即刻到手</h2>
        <p class="slogan-sub">正品保障 · 极速发货 · 售后无忧</p>
        <ul class="points">
          <li>精选好物，一站购齐</li>
          <li>超时未支付自动关闭并归还库存</li>
        </ul>
      </div>
    </aside>

    <!-- 右：登录表单 -->
    <section class="form-side">
      <div class="form-card">
        <h1 class="title">欢迎登录</h1>
        <p class="subtitle">登录后即可下单、查看订单与购物车</p>

        <el-form label-position="top" @submit.prevent="handleLogin">
          <el-form-item label="用户名">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              clearable
            />
          </el-form-item>

          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
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

        <div class="footer">
          <span class="hint">还没有账号？</span>
          <el-button link type="primary" @click="goRegister">免费注册</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 100vh;
  background: #fff;
}

/* ---------- 左侧品牌区 ---------- */
.side {
  position: relative;
  background-image: url('../../assets/1.jpg');
  background-size: cover;
  background-position: center;
  color: #fff;
  overflow: hidden;
}
/* 品牌色渐变遮罩：让图片和品牌统一，同时保证文字可读 */
.side::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(150deg, rgba(255, 90, 0, 0.88), rgba(255, 43, 0, 0.72));
}
.side-inner {
  position: relative;
  z-index: 1;
  height: 100%;
  padding: 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 40px;
}
.brand-mark {
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.5px;
}
.brand-text {
  font-size: 14px;
  opacity: 0.9;
}

.slogan {
  margin: 0 0 10px;
  font-size: 34px;
  font-weight: 800;
  letter-spacing: 1px;
}
.slogan-sub {
  margin: 0 0 28px;
  font-size: 14px;
  opacity: 0.9;
}
.points {
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 13px;
  line-height: 2;
  opacity: 0.95;
}
.points li::before {
  content: '✓';
  margin-right: 8px;
  font-weight: 700;
}

/* ---------- 右侧表单 ---------- */
.form-side {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.form-card {
  width: 360px;
  max-width: 100%;
}

.title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 700;
  color: #222;
}
.subtitle {
  margin: 0 0 28px;
  font-size: 13px;
  color: var(--tk-text-sub);
}

.submit {
  width: 100%;
  height: 44px;
  font-size: 15px;
  margin-top: 4px;
}

.footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 20px;
}

.hint {
  font-size: 13px;
  color: var(--tk-text-sub);
}

/* 窄屏：隐藏左侧图，只留表单 */
@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
    background: linear-gradient(160deg, #fff7f2, #fff);
  }
  .side {
    display: none;
  }
}
</style>
