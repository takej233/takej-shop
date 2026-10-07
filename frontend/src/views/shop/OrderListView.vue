<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ShopHeader from '@/components/ShopHeader.vue'
import { listOrders } from '@/api/order'
import { errorMessage } from '@/api/http'
import { ORDER_STATUS_TEXT } from '@/types/constants'
import type { OrderInfo } from '@/types/api'

const router = useRouter()

const orders = ref<OrderInfo[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    orders.value = await listOrders()
  } catch (error) {
    ElMessage.error(errorMessage(error, '订单加载失败'))
  } finally {
    loading.value = false
  }
}

onMounted(load)

function formatPrice(amount: number) {
  return Number(amount).toFixed(2)
}

/** 状态值 → Element Plus 标签颜色 */
function statusTagType(status: number) {
  if (status === 0) return 'warning'
  if (status === 1) return 'primary'
  if (status === 3) return 'success'
  return 'info'
}

/** 后端返回不带时区的 ISO 字符串，按本地时间展示即可 */
function formatTime(time: string) {
  return time ? time.replace('T', ' ').slice(0, 19) : '—'
}
</script>

<template>
  <div class="tk-page">
    <ShopHeader />

    <main v-loading="loading" class="tk-container">
      <div class="tk-title">我的订单</div>

      <el-empty v-if="!loading && orders.length === 0" description="还没有订单" />

      <div v-else class="tk-panel">
        <el-table :data="orders" border>
        <el-table-column prop="orderNo" label="订单号" min-width="240" />

        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span class="amount">¥{{ formatPrice(row.totalAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ ORDER_STATUS_TEXT[row.status] ?? '未知' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="下单时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="支付截止" width="180">
          <template #default="{ row }">{{ formatTime(row.expireTime) }}</template>
        </el-table-column>

          <el-table-column label="操作" width="110" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="router.push(`/orders/${row.id}`)">
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* 金额列用价格红，和普通文本区分开 */
.amount {
  color: var(--tk-price);
  font-weight: 600;
}
</style>
