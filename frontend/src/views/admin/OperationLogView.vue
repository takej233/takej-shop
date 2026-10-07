<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminListOperationLogs } from '@/api/admin'
import { errorMessage } from '@/api/http'
import { OPERATION_TEXT } from '@/types/constants'
import type { AdminOperationLog } from '@/types/api'

const logs = ref<AdminOperationLog[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    logs.value = await adminListOperationLogs()
  } catch (error) {
    ElMessage.error(errorMessage(error, '日志加载失败'))
  } finally {
    loading.value = false
  }
}

onMounted(load)

function formatTime(time: string) {
  return time ? time.replace('T', ' ').slice(0, 19) : '—'
}

/** 只有「真正改变了系统状态」的操作才会留下日志：
 *  被 403 拦下的、被 409 拒绝的，都不会记录。 */
function operationText(code: string) {
  return OPERATION_TEXT[code] ?? code
}

function targetText(row: AdminOperationLog) {
  if (row.targetType === 'PRODUCT') return `商品 #${row.targetId}`
  if (row.targetType === 'ORDER') return `订单 #${row.targetId}`
  return `${row.targetType} #${row.targetId}`
}
</script>

<template>
  <div>
    <div class="toolbar">
      <h2 class="title">操作日志</h2>
      <span class="sub">最近 100 条，最新在前</span>
    </div>

    <el-table v-loading="loading" :data="logs" border>
      <el-table-column prop="id" label="ID" width="70" />

      <el-table-column label="管理员" width="110">
        <template #default="{ row }">#{{ row.adminId }}</template>
      </el-table-column>

      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-tag size="small">{{ operationText(row.operation) }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="目标对象" width="140">
        <template #default="{ row }">{{ targetText(row) }}</template>
      </el-table-column>

      <el-table-column label="详情" min-width="220">
        <template #default="{ row }">{{ row.detail || '—' }}</template>
      </el-table-column>

      <el-table-column label="时间" width="175">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.title {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.sub {
  font-size: 13px;
  color: #909399;
}
</style>
