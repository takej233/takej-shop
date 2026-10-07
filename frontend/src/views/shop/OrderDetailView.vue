<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ShopHeader from '@/components/ShopHeader.vue'
import {
  cancelOrder,
  completeOrder,
  getOrderDetail,
  payOrder
} from '@/api/order'
import { errorMessage } from '@/api/http'
import { ORDER_STATUS, ORDER_STATUS_TEXT } from '@/types/constants'
import type { OrderDetail } from '@/types/api'

const route = useRoute()
const router = useRouter()

const orderId = Number(route.params.id)
const detail = ref<OrderDetail | null>(null)
const loading = ref(false)
const operating = ref(false)

const order = computed(() => detail.value?.order ?? null)
const items = computed(() => detail.value?.items ?? [])

/** 按订单状态决定哪些操作可用 */
const canPay = computed(() => order.value?.status === ORDER_STATUS.PENDING_PAYMENT)
const canCancel = computed(() => order.value?.status === ORDER_STATUS.PENDING_PAYMENT)
const canComplete = computed(() => order.value?.status === ORDER_STATUS.PAID)

async function load() {
  loading.value = true
  try {
    detail.value = await getOrderDetail(orderId)
  } catch (error) {
    ElMessage.error(errorMessage(error, '订单加载失败'))
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function run(action: () => Promise<unknown>, successText: string) {
  operating.value = true
  try {
    await action()
    ElMessage.success(successText)
    // 操作完成后重新拉一次，让状态显示和后端保持一致
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error, '操作失败'))
    // 订单状态可能已被定时任务改掉，刷新一次再展示
    await load()
  } finally {
    operating.value = false
  }
}

async function handlePay() {
  await run(() => payOrder(orderId), '支付成功')
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定取消这笔订单吗？取消后库存会归还。', '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  await run(() => cancelOrder(orderId), '订单已取消')
}

async function handleComplete() {
  await run(() => completeOrder(orderId), '已确认收货')
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
  <div class="tk-page">
    <ShopHeader />

    <main v-loading="loading" class="tk-container">
      <el-button link type="primary" class="back" @click="router.push('/orders')">
        ← 返回订单列表
      </el-button>

      <div v-if="order" class="tk-panel">
        <div class="card-head">
          <span class="title">订单详情</span>
          <el-tag :type="statusTagType(order.status)" effect="dark">
            {{ ORDER_STATUS_TEXT[order.status] ?? '未知' }}
          </el-tag>
        </div>

        <el-descriptions :column="2" border class="desc">
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单 ID">{{ order.id }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">
            {{ formatTime(order.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="支付截止时间">
            {{ formatTime(order.expireTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="订单金额">
            <span class="amount">¥{{ formatPrice(order.totalAmount) }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <h3 class="section-title">商品明细</h3>

        <el-table :data="items" border>
          <el-table-column prop="productName" label="商品名称" min-width="200" />
          <el-table-column label="单价" width="120" align="right">
            <template #default="{ row }">¥{{ formatPrice(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="90" align="center" />
          <el-table-column label="小计" width="140" align="right">
            <template #default="{ row }">¥{{ formatPrice(row.subtotal) }}</template>
          </el-table-column>
        </el-table>

        <el-alert
          v-if="order.status === ORDER_STATUS.PENDING_PAYMENT"
          type="info"
          show-icon
          :closable="false"
          class="tip"
          title="超过 30 分钟未支付，订单会被系统自动关闭并归还库存"
        />

        <div class="ops">
          <el-button
            v-if="canPay"
            type="primary"
            :loading="operating"
            @click="handlePay"
          >
            支付
          </el-button>

          <el-button
            v-if="canCancel"
            :loading="operating"
            @click="handleCancel"
          >
            取消订单
          </el-button>

          <el-button
            v-if="canComplete"
            type="success"
            :loading="operating"
            @click="handleComplete"
          >
            确认收货
          </el-button>

          <span v-if="!canPay && !canCancel && !canComplete" class="no-op">
            该订单已结束，没有可执行的操作
          </span>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.back {
  margin-bottom: 12px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--tk-border);
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.desc {
  margin-bottom: 4px;
}

.amount {
  color: var(--tk-price);
  font-size: 18px;
  font-weight: 700;
}

.section-title {
  margin: 24px 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.tip {
  margin-top: 16px;
}

.ops {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
}

.btn-pay {
  min-width: 120px;
}

.no-op {
  font-size: 13px;
  color: var(--tk-text-sub);
}
</style>
