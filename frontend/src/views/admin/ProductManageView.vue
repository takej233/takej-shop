<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAdminStore } from '@/stores/admin'
import {
  adminAdjustStock,
  adminCreateProduct,
  adminDeleteProduct,
  adminListProducts,
  adminListStockLogs,
  adminUpdateProduct,
  adminUpdateProductStatus
} from '@/api/admin'
import { errorMessage, isApiError } from '@/api/http'
import { PERMISSION, PRODUCT_STATUS, STOCK_SOURCE_TEXT } from '@/types/constants'
import type { AdminProductItem, AdminStockLogItem } from '@/types/api'

const adminStore = useAdminStore()

const products = ref<AdminProductItem[]>([])
const loading = ref(false)
const submitting = ref(false)

// ===== 新增 / 编辑 商品 =====
const editVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  subtitle: '' as string | null,
  mainImage: '' as string | null,
  price: 0,
  stock: 0,
  sort: 0 as number | null
})

async function load() {
  loading.value = true
  try {
    products.value = await adminListProducts()
  } catch (error) {
    ElMessage.error(errorMessage(error, '商品加载失败'))
  } finally {
    loading.value = false
  }
}

onMounted(load)

function resetForm() {
  editingId.value = null
  form.name = ''
  form.subtitle = null
  form.mainImage = null
  form.price = 0
  form.stock = 0
  form.sort = 0
}

function openCreate() {
  resetForm()
  editVisible.value = true
}

function openEdit(row: AdminProductItem) {
  // 用列表行回填表单。
  // 注意：subtitle / mainImage / sort 依赖后端把这些字段放进 AdminProductItem，
  // 否则这里回填不到，保存时会把原值清成空。
  editingId.value = row.id
  form.name = row.name
  form.subtitle = row.subtitle ?? null
  form.mainImage = row.mainImage ?? null
  form.price = Number(row.price)
  form.stock = row.stock
  form.sort = row.sort ?? 0
  editVisible.value = true
}

async function submitEdit() {
  if (!form.name.trim()) {
    ElMessage.warning('商品名称不能为空')
    return
  }
  if (form.price <= 0) {
    ElMessage.warning('商品价格必须大于 0')
    return
  }

  submitting.value = true
  try {
    if (editingId.value === null) {
      await adminCreateProduct({
        name: form.name.trim(),
        subtitle: form.subtitle || null,
        mainImage: form.mainImage || null,
        price: form.price,
        stock: form.stock,
        sort: form.sort
      })
      ElMessage.success('商品已创建')
    } else {
      // 修改商品不带库存：库存只能通过「调整库存」单独改
      await adminUpdateProduct(editingId.value, {
        name: form.name.trim(),
        subtitle: form.subtitle || null,
        mainImage: form.mainImage || null,
        price: form.price,
        sort: form.sort
      })
      ElMessage.success('商品已更新')
    }

    editVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error, '保存失败'))
  } finally {
    submitting.value = false
  }
}

// ===== 上下架 =====
async function handleToggleStatus(row: AdminProductItem) {
  const next = row.status === PRODUCT_STATUS.ON ? PRODUCT_STATUS.OFF : PRODUCT_STATUS.ON
  try {
    await adminUpdateProductStatus(row.id, next)
    ElMessage.success(next === PRODUCT_STATUS.ON ? '已上架' : '已下架')
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error, '操作失败'))
  }
}

// ===== 删除 =====
async function handleDelete(row: AdminProductItem) {
  try {
    await ElMessageBox.confirm(`确定删除商品「${row.name}」吗？`, '提示', {
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await adminDeleteProduct(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    // 409：该商品还有未完成的订单，后端中文提示直接展示
    ElMessage.error(errorMessage(error, '删除失败'))
  }
}

// ===== 调整库存 =====
const stockVisible = ref(false)
const stockTarget = ref<AdminProductItem | null>(null)
const stockForm = reactive({
  changeQuantity: 0,
  reason: ''
})

function openAdjustStock(row: AdminProductItem) {
  stockTarget.value = row
  stockForm.changeQuantity = 0
  stockForm.reason = ''
  stockVisible.value = true
}

async function submitAdjustStock() {
  if (!stockTarget.value) return
  if (stockForm.changeQuantity === 0) {
    ElMessage.warning('变动数量不能为 0')
    return
  }

  submitting.value = true
  try {
    await adminAdjustStock(stockTarget.value.id, {
      changeQuantity: stockForm.changeQuantity,
      reason: stockForm.reason || null
    })
    ElMessage.success('库存已调整')
    stockVisible.value = false
    await load()
  } catch (error) {
    // 409：调整后库存会变成负数
    ElMessage.error(errorMessage(error, '调整失败'))
  } finally {
    submitting.value = false
  }
}

// ===== 库存变动记录 =====
const logVisible = ref(false)
const logTarget = ref<AdminProductItem | null>(null)
const stockLogs = ref<AdminStockLogItem[]>([])
const logSource = ref<number | null>(null)

async function openStockLogs(row: AdminProductItem) {
  logTarget.value = row
  logSource.value = null
  await loadStockLogs()
  logVisible.value = true
}

async function loadStockLogs() {
  if (!logTarget.value) return
  try {
    stockLogs.value = await adminListStockLogs(logTarget.value.id, logSource.value)
  } catch (error) {
    // 商品不存在时后端返回 404，这里不当成页面级错误
    if (isApiError(error) && error.status === 404) {
      stockLogs.value = []
      return
    }
    ElMessage.error(errorMessage(error, '记录加载失败'))
  }
}

function formatPrice(price: number) {
  return Number(price).toFixed(2)
}

function formatTime(time: string) {
  return time ? time.replace('T', ' ').slice(0, 19) : '—'
}
</script>

<template>
  <div>
    <div class="toolbar">
      <h2 class="title">商品管理</h2>
      <el-button
        v-if="adminStore.hasPermission(PERMISSION.PRODUCT_CREATE)"
        type="primary"
        @click="openCreate"
      >
        新增商品
      </el-button>
    </div>

    <el-table v-loading="loading" :data="products" border>
      <el-table-column prop="id" label="ID" width="70" />

      <el-table-column prop="name" label="商品名称" min-width="180">
        <template #default="{ row }">
          <div class="name-cell">
            <span>{{ row.name }}</span>
            <el-tag v-if="row.deleted === 1" type="danger" size="small">已删除</el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="价格" width="110" align="right">
        <template #default="{ row }">¥{{ formatPrice(row.price) }}</template>
      </el-table-column>

      <el-table-column prop="stock" label="库存" width="90" align="center">
        <template #default="{ row }">
          <span :class="{ 'stock-empty': row.stock <= 0 }">{{ row.stock }}</span>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" min-width="300">
        <template #default="{ row }">
          <!-- 已逻辑删除的商品只剩「库存记录」可看：
               编辑 / 上下架 / 调库存 / 删除 这些接口都会返回 404，
               留在这里只会让管理员点了才报错。 -->
          <template v-if="row.deleted === 0">
            <el-button
              v-if="adminStore.hasPermission(PERMISSION.PRODUCT_UPDATE)"
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>

            <el-button
              v-if="adminStore.hasPermission(PERMISSION.PRODUCT_STATUS)"
              link
              type="primary"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>

            <el-button
              v-if="adminStore.hasPermission(PERMISSION.STOCK_ADJUST)"
              link
              type="primary"
              @click="openAdjustStock(row)"
            >
              调整库存
            </el-button>

            <el-button
              v-if="adminStore.hasPermission(PERMISSION.PRODUCT_DELETE)"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>

          <el-button link type="primary" @click="openStockLogs(row)">
            库存记录
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增 / 编辑商品 -->
    <el-dialog
      v-model="editVisible"
      :title="editingId === null ? '新增商品' : '修改商品'"
      width="520px"
    >
      <el-form label-width="90px">
        <el-form-item label="商品名称" required>
          <el-input
            v-model="form.name"
            maxlength="100"
            show-word-limit
            placeholder="最长 100 个字符"
          />
        </el-form-item>

        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" maxlength="255" show-word-limit />
        </el-form-item>

        <el-form-item label="主图地址">
          <el-input v-model="form.mainImage" placeholder="图片 URL" />
        </el-form-item>

        <el-form-item label="价格" required>
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" />
        </el-form-item>

        <el-form-item v-if="editingId === null" label="初始库存" required>
          <el-input-number v-model="form.stock" :min="0" :step="1" />
        </el-form-item>

        <el-form-item v-else label="库存">
          <div class="readonly-stock">
            <span>{{ form.stock }}</span>
            <span class="hint">库存请用「调整库存」修改，避免并发扣减被覆盖</span>
          </div>
        </el-form-item>

        <el-form-item label="排序值">
          <el-input-number v-model="form.sort" :min="0" :step="1" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 调整库存 -->
    <el-dialog v-model="stockVisible" title="调整库存" width="420px">
      <el-alert
        type="info"
        show-icon
        :closable="false"
        class="tip"
        title="填的是增量，不是目标值：+5 表示加 5 件，-3 表示减 3 件"
      />

      <el-form label-width="90px">
        <el-form-item label="商品">
          <span>{{ stockTarget?.name }}（当前库存 {{ stockTarget?.stock }}）</span>
        </el-form-item>

        <el-form-item label="变动数量" required>
          <el-input-number v-model="stockForm.changeQuantity" :step="1" />
        </el-form-item>

        <el-form-item label="调整原因">
          <el-input
            v-model="stockForm.reason"
            type="textarea"
            :rows="2"
            maxlength="255"
            show-word-limit
            placeholder="建议填写，会记入审计记录"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="stockVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAdjustStock">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 库存变动记录 -->
    <el-dialog v-model="logVisible" title="库存变动记录" width="760px">
      <div class="log-toolbar">
        <span class="log-title">{{ logTarget?.name }}</span>

        <el-select
          v-model="logSource"
          placeholder="全部来源"
          clearable
          size="small"
          style="width: 150px"
          @change="loadStockLogs"
        >
          <el-option label="人工调整" :value="1" />
          <el-option label="下单扣减" :value="2" />
          <el-option label="订单归还" :value="3" />
        </el-select>
      </div>

      <el-table :data="stockLogs" border size="small">
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            {{ STOCK_SOURCE_TEXT[row.source] ?? '未知' }}
          </template>
        </el-table-column>

        <el-table-column label="变动" width="90" align="center">
          <template #default="{ row }">
            <span :class="row.changeQuantity >= 0 ? 'plus' : 'minus'">
              {{ row.changeQuantity >= 0 ? '+' : '' }}{{ row.changeQuantity }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="变动前" prop="stockBefore" width="90" align="center" />
        <el-table-column label="变动后" prop="stockAfter" width="90" align="center" />

        <el-table-column label="原因" min-width="140">
          <template #default="{ row }">{{ row.reason || '—' }}</template>
        </el-table-column>

        <el-table-column label="操作人" width="100">
          <template #default="{ row }">
            {{ row.operatorId == null ? '系统' : `#${row.operatorId}` }}
          </template>
        </el-table-column>

        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="logVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.title {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stock-empty {
  color: #e6a23c;
  font-weight: 600;
}

.readonly-stock {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hint {
  font-size: 12px;
  color: #909399;
}

.tip {
  margin-bottom: 16px;
}

.log-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.log-title {
  font-size: 14px;
  color: #303133;
}

.plus {
  color: #67c23a;
  font-weight: 600;
}

.minus {
  color: #f56c6c;
  font-weight: 600;
}
</style>
