<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'
import { errorMessage, isApiError } from '@/api/http'

const router = useRouter()

const form = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)

async function handleRegister() {
  // 前端先做一遍和后端一致的校验，避免为了一个明显的错误白跑一次请求
  const username = form.username.trim()

  if (!username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    await register(username, form.password)
    ElMessage.success('注册成功，请登录')

    // 关键：注册接口只返回新用户 ID，不会自动登录。
    // 带着用户名跳回登录页并预填，省得用户再敲一遍。
    await router.replace({
      path: '/login',
      query: { username }
    })
  } catch (error) {
    // 后端对"用户名已存在"返回 409，但它给的提示是英文的
    // username already exists，这里换成中文。
    if (isApiError(error) && error.status === 409) {
      ElMessage.error('该用户名已被占用，换一个试试')
    } else {
      ElMessage.error(errorMessage(error, '注册失败'))
    }
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="auth-page">
    <!-- 左：品牌展示区（和登录页同一套视觉） -->
    <aside class="side">
      <div class="side-inner">
        <div class="brand">
          <span class="brand-mark">takej</span>
          <span class="brand-text">商城</span>
        </div>
        <h2 class="slogan">加入 takej 商城</h2>
        <p class="slogan-sub">注册即享新人好物，随时随地查看订单</p>
        <ul class="points">
          <li>一个账号，通购全场</li>
          <li>购物车、订单、资料统一管理</li>
        </ul>
      </div>
    </aside>

    <!-- 右：注册表单 -->
    <section class="form-side">
      <div class="form-card">
        <h1 class="title">创建账号</h1>
        <p class="subtitle">只需用户名和密码，30 秒完成注册</p>

        <el-form label-position="top" @submit.prevent="handleRegister">
          <el-form-item label="用户名">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              maxlength="50"
              clearable
            />
          </el-form-item>

          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="至少 6 位"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item label="确认密码">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="再输入一次密码"
              size="large"
              show-password
              @keyup.enter="handleRegister"
            />
          </el-form-item>

          <el-button
            class="tk-btn-main submit"
            size="large"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form>

        <div class="footer">
          <span class="hint">已经有账号了？</span>
          <el-button link type="primary" @click="goLogin">去登录</el-button>
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
