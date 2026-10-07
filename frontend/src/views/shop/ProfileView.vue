<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ShopHeader from '@/components/ShopHeader.vue'
import { getMyProfile, updateMyProfile } from '@/api/user'
import { errorMessage, isApiError } from '@/api/http'

const form = reactive({
  nickname: '',
  avatar: '' as string | null,
  gender: 0 as number | null,
  phone: '' as string | null
})

const loading = ref(false)
const saving = ref(false)
/** 历史账号可能还没有资料记录，后端会返回 404 */
const notCreated = ref(false)
const loaded = ref(false)

async function load() {
  loading.value = true
  try {
    const profile = await getMyProfile()
    form.nickname = profile.nickname
    form.avatar = profile.avatar
    form.gender = profile.gender
    form.phone = profile.phone
    notCreated.value = false
  } catch (error) {
    if (isApiError(error) && error.status === 404) {
      // 资料还不存在：给一个空表单让用户补全，不当成错误
      notCreated.value = true
      form.nickname = ''
      form.gender = 0
    } else {
      ElMessage.error(errorMessage(error, '资料加载失败'))
    }
  } finally {
    loading.value = false
    loaded.value = true
  }
}

onMounted(load)

async function handleSave() {
  if (!form.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }

  saving.value = true
  try {
    await updateMyProfile({
      nickname: form.nickname.trim(),
      avatar: form.avatar || null,
      gender: form.gender,
      phone: form.phone || null
    })
    ElMessage.success('资料已保存')
    notCreated.value = false
  } catch (error) {
    ElMessage.error(errorMessage(error, '保存失败'))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="tk-page">
    <ShopHeader />

    <main v-loading="loading" class="tk-container">
      <div class="tk-title">个人资料</div>

      <el-alert
        v-if="notCreated"
        type="info"
        show-icon
        :closable="false"
        class="tip"
        title="还没有个人资料，填一下就能补全"
      />

      <div v-if="loaded" class="tk-panel profile">
        <el-form label-width="90px">
          <el-form-item label="昵称" required>
            <el-input
              v-model="form.nickname"
              placeholder="请输入昵称，最长 50 个字符"
              maxlength="50"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="头像地址">
            <div class="avatar-row">
              <div class="avatar">
                <img v-if="form.avatar" :src="form.avatar" alt="头像" />
                <span v-else>{{ form.nickname.slice(0, 1) || '用' }}</span>
              </div>
              <el-input v-model="form.avatar" placeholder="选填，图片 URL 地址" />
            </div>
          </el-form-item>

          <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
              <el-radio :value="0">未知</el-radio>
              <el-radio :value="1">男</el-radio>
              <el-radio :value="2">女</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="手机号">
            <el-input
              v-model="form.phone"
              placeholder="选填"
              maxlength="20"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <el-button class="tk-btn-main" :loading="saving" @click="handleSave">
              保存
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </main>
  </div>
</template>

<style scoped>
.profile {
  max-width: 640px;
}

.tip {
  margin-bottom: 16px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.avatar {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #ff7a2f, #ff2b00);
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
