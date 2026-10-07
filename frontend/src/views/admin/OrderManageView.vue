<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAdminStore } from '@/stores/admin'
import {
  adminCloseOrder,
  adminGetOrderDetail,
  adminListOrders
} from '@/api/admin'
import { errorMessage } from '@/api/http'
import { ORDER_STATUS_TEXT, PERMISSION } from '@/types/constants'
import type { AdminOrderDetail, AdminOrderItem } from '@/types/api'

const adminStore = useAdminStore()

const orders = ref<AdminOrderItem[]>([])
const loading = ref(false)
const operating = ref(false)

const detailVisible = ref(false)
const detail = ref<AdminOrderDetail | null>(null)

async function load() {
  loading.value = true
  try {
    orders.value = await adminListOrders()
  } catch (error) {
    ElMessage.error(errorMessage(error, '订单加载失败'))
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function openDetail(orderId: number) {
  try {
    detail.value = await adminGetOrderDetail(orderId)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(errorMessage(error, '详情加载失败'))
  }
}

async function handleClose(row: AdminOrderItem) {
  try {
    await ElMessageBox.confirm(
      `确定关闭订单 ${row.orderNo} 吗？关闭后会归还该订单占用的库存。`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }

  operating.value = true
  try {
    await adminCloseOrder(row.id)
    ElMessage.success('订单已关闭，库存已归还')
    await load()
  } catch (error) {
    // 409：订单不是待支付状态（可能刚被用户支付或取消），刷新列表再说
    ElMessage.error(errorMessage(error, '关闭失败'))
    await load()
  } finally {
    operating.value = false
  }
}

function formatPrice(amount: number) {
  return Number(amount).toFixed(2)
}

function formatTime(time: string) {
  return time ? time.replace('T', ' ').slice(0, 19) : '—'
}

function statusTagType(status: number) {
  if (status === 0) return 'warning'
  if (status === 1) return 'primary'
  if (status === 3) return 'success'
  return 'info'
}
</script>

<template>
  <div>
    <div class="toolbar">
      <h2 class="title">订单管理</h2>
      <span class="sub">全部用户的订单，最新在前</span>
    </div>

    <el-table v-loading="loading" :data="orders" border>
      <el-table-column prop="orderNo" label="订单号" min-width="230" />

      <el-table-column label="下单用户" width="110" align="center">
        <template #default="{ row }">#{{ row.userId }}</template>
      </el-table-column>

      <el-table-column label="金额" width="120" align="right">
        <template #default="{ row }">¥{{ formatPrice(row.totalAmount) }}</template>
      </el-table-column>

      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">
            {{ ORDER_STATUS_TEXT[row.status] ?? '未知' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="下单时间" width="175">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>

      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>

          <el-button
            v-if="
              adminStore.hasPermission(PERMISSION.CLOSE_ORDER) && row.status === 0
            "
            link
            type="danger"
            :loading="operating"
            @click="handleClose(row)"
          >
            关闭
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" title="订单详情" width="760px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户 ID">{{ detail.order.userId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ ORDER_STATUS_TEXT[detail.order.status] ?? '未知' }}
        </el-descriptions-item>
        <el-descriptions-item label="金额">
          ¥{{ formatPrice(detail.order.totalAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="下单时间">
          {{ formatTime(detail.order.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="支付截止">
          {{ formatTime(detail.order.expireTime) }}
        </el-descriptions-item>
      </el-descriptions>

      <h3 class="section-title">商品明细</h3>

      <el-table :data="detail?.items ?? []" border size="small">
        <el-table-column prop="productName" label="商品名称" min-width="180" />
        <el-table-column label="单价" width="110" align="right">
          <template #default="{ row }">¥{{ formatPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
        <el-table-column label="小计" width="120" align="right">
          <template #default="{ row }">¥{{ formatPrice(row.subtotal) }}</template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
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

.section-title {
  margin: 20px 0 12px;
  font-size: 15px;
  color: #303133;
}
</style>
