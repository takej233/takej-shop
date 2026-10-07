<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ShopHeader from '@/components/ShopHeader.vue'
import { deleteItem, deleteSelected, listItems, updateChecked, updateQuantity } from '@/api/cart'
import { listProductsByIds } from '@/api/product'
import { createOrderFromCart, newRequestId } from '@/api/order'
import { errorMessage } from '@/api/http'
import { useCartStore } from '@/stores/cart'
import type { CartItem, Product } from '@/types/api'

const router = useRouter()
const cartStore = useCartStore()

/** 购物车行 = 后端数据 + 补全出来的商品信息 */
interface CartRow extends CartItem {
  product: Product | null
  /** 商品已下架或已删除，查不到了 */
  unavailable: boolean
}

const rows = ref<CartRow[]>([])
const loading = ref(false)
const submitting = ref(false)

/** 金额一律用「整数分」计算，避免 0.1 + 0.2 这种浮点误差 */
function toCents(price: number) {
  return Math.round(Number(price) * 100)
}
function fromCents(cents: number) {
  return (cents / 100).toFixed(2)
}

/** 只有「可用 + 已勾选」的商品才参与合计 */
const availableCheckedRows = computed(() =>
  rows.value.filter((r) => !r.unavailable && r.checked === 1)
)

const totalCents = computed(() =>
  availableCheckedRows.value.reduce(
    (sum, r) => sum + toCents(r.product!.price) * r.quantity,
    0
  )
)

const hasUnavailable = computed(() => rows.value.some((r) => r.unavailable))

/** 可操作（未下架）的行，用于「全选」判断 */
const selectableRows = computed(() => rows.value.filter((r) => !r.unavailable))
const allChecked = computed(
  () =>
    selectableRows.value.length > 0 &&
    selectableRows.value.every((r) => r.checked === 1)
)

async function load() {
  loading.value = true
  try {
    const items = await listItems()

    // 后端只给了 productId，商品名/价格/库存要另外补全。
    // 走批量接口一次拿全，不再是一件商品发一次请求。
    const ids = items.map((item) => item.productId)
    const products = ids.length > 0 ? await listProductsByIds(ids) : []

    const productMap = new Map<number, Product>()
    products.forEach((p) => productMap.set(p.id, p))

    // 批量接口只返回上架且未删除的商品，
    // 查不到的就是已下架或已删除，属于正常业务分支，不是错误。
    const enriched: CartRow[] = items.map((item) => {
      const product = productMap.get(item.productId) ?? null
      return { ...item, product, unavailable: product === null }
    })

    // 已下架但数据库里还勾着的项，后端结算时会把它一起拿去下单 → 整单 404。
    // 所以这里静默把它取消勾选，避免用户结算时撞一个莫名其妙的失败。
    await Promise.all(
      enriched
        .filter((r) => r.unavailable && r.checked === 1)
        .map((r) =>
          updateChecked(r.productId, 0)
            .then(() => { r.checked = 0 })
            .catch(() => { /* 取消失败不阻塞页面展示 */ })
        )
    )

    rows.value = enriched
    cartStore.count = rows.value.length
  } catch (error) {
    ElMessage.error(errorMessage(error, '购物车加载失败'))
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function handleCheckedChange(row: CartRow, checked: boolean) {
  const next = checked ? 1 : 0
  const previous = row.checked
  row.checked = next

  try {
    await updateChecked(row.productId, next)
  } catch (error) {
    row.checked = previous // 失败就回滚，别让界面和后端不一致
    ElMessage.error(errorMessage(error, '修改勾选状态失败'))
  }
}

async function handleCheckAll(checked: boolean) {
  const next = checked ? 1 : 0
  try {
    await Promise.all(selectableRows.value.map((r) => updateChecked(r.productId, next)))
    selectableRows.value.forEach((r) => { r.checked = next })
  } catch (error) {
    ElMessage.error(errorMessage(error, '修改勾选状态失败'))
  }
}

async function handleQuantityChange(row: CartRow, value: number | undefined) {
  if (!value || value < 1) return

  const previous = row.quantity
  row.quantity = value

  try {
    await updateQuantity(row.productId, value)
  } catch (error) {
    row.quantity = previous
    ElMessage.error(errorMessage(error, '修改数量失败'))
  }
}

async function handleRemove(row: CartRow) {
  try {
    await deleteItem(row.productId)
    rows.value = rows.value.filter((r) => r.productId !== row.productId)
    cartStore.count = rows.value.length
    ElMessage.success('已移除')
  } catch (error) {
    ElMessage.error(errorMessage(error, '移除失败'))
  }
}

async function handleRemoveSelected() {
  if (availableCheckedRows.value.length === 0) {
    ElMessage.warning('请先勾选要删除的商品')
    return
  }

  try {
    await ElMessageBox.confirm('确定删除所有已勾选的商品吗？', '提示', {
      type: 'warning'
    })
  } catch {
    return // 用户点了取消
  }

  try {
    await deleteSelected()
    await load()
    ElMessage.success('已删除')
  } catch (error) {
    ElMessage.error(errorMessage(error, '删除失败'))
  }
}

/**
 * 结算：把已勾选的商品下成一笔订单。
 * 成功后后端会自动清空已勾选的购物车项，所以直接跳订单列表。
 */
async function handleCheckout() {
  if (availableCheckedRows.value.length === 0) {
    ElMessage.warning('请先勾选要结算的商品')
    return
  }

  submitting.value = true
  try {
    // 带上幂等号：重复点击不会创建第二笔订单，也不会重复扣库存
    const orderId = await createOrderFromCart(newRequestId())
    ElMessage.success(`下单成功，订单号 ${orderId}`)
    await router.push('/orders')
  } catch (error) {
    // 库存不足是 409，后端中文提示直接透传；
    // 商品下架是 404，同样直接展示给用户可以看懂的提示。
    ElMessage.error(errorMessage(error, '下单失败'))
  } finally {
    submitting.value = false
  }
}

function formatPrice(price: number) {
  return Number(price).toFixed(2)
}
</script>

<template>
  <div class="tk-page">
    <ShopHeader />

    <main v-loading="loading" class="tk-container">
      <div class="tk-title">我的购物车</div>

      <el-alert
        v-if="hasUnavailable"
        type="warning"
        show-icon
        :closable="false"
        title="购物车里有已下架的商品，已自动取消勾选，请先移除再结算"
        class="alert"
      />

      <el-empty v-if="!loading && rows.length === 0" description="购物车是空的，去挑点东西吧" />

      <template v-else>
        <!-- 表头 -->
        <div class="cart-head">
          <div class="col-check">
            <el-checkbox
              :model-value="allChecked"
              :disabled="selectableRows.length === 0"
              @change="(v: any) => handleCheckAll(Boolean(v))"
            />
          </div>
          <div class="col-goods">商品</div>
          <div class="col-price">单价</div>
          <div class="col-qty">数量</div>
          <div class="col-sub">小计</div>
          <div class="col-op">操作</div>
        </div>

        <!-- 列表 -->
        <div class="cart-body">
          <div v-for="row in rows" :key="row.productId" class="cart-row">
            <div class="col-check">
              <el-checkbox
                :model-value="row.checked === 1"
                :disabled="row.unavailable"
                @change="(v: any) => handleCheckedChange(row, Boolean(v))"
              />
            </div>

            <div class="col-goods">
              <div class="thumb">
                <img
                  v-if="row.product?.mainImage"
                  :src="row.product.mainImage"
                  :alt="row.product.name"
                />
                <span v-else class="tk-thumb-empty">无图</span>
              </div>
              <div class="goods-info">
                <div class="name-line">
                  <span class="name">{{ row.product?.name ?? '商品已下架' }}</span>
                  <el-tag v-if="row.unavailable" type="info" size="small">已下架</el-tag>
                  <el-tag v-else-if="row.product!.stock <= 0" type="warning" size="small">
                    售罄
                  </el-tag>
                </div>
                <div class="sub">{{ row.product?.subtitle || '' }}</div>
              </div>
            </div>

            <div class="col-price">
              <template v-if="row.product">¥{{ formatPrice(row.product.price) }}</template>
              <template v-else>—</template>
            </div>

            <div class="col-qty">
              <el-input-number
                :model-value="row.quantity"
                :min="1"
                :disabled="row.unavailable"
                size="small"
                @change="(v: number | undefined) => handleQuantityChange(row, v)"
              />
            </div>

            <div class="col-sub">
              <template v-if="row.product">
                ¥{{ fromCents(toCents(row.product.price) * row.quantity) }}
              </template>
              <template v-else>—</template>
            </div>

            <div class="col-op">
              <el-button link type="danger" @click="handleRemove(row)">移除</el-button>
            </div>
          </div>
        </div>

        <!-- 结算条 -->
        <div class="cart-bar">
          <el-button link type="danger" @click="handleRemoveSelected">
            删除已勾选
          </el-button>

          <div class="summary">
            <span class="count">已选 {{ availableCheckedRows.length }} 件</span>
            <span class="total">
              合计：<em>¥</em>{{ fromCents(totalCents) }}
            </span>
            <el-button
              class="tk-btn-main checkout"
              size="large"
              :loading="submitting"
              :disabled="availableCheckedRows.length === 0"
              @click="handleCheckout"
            >
              去结算
            </el-button>
          </div>
        </div>
      </template>
    </main>
  </div>
</template>

<style scoped>
.alert {
  margin-bottom: 16px;
}

/* ---------- 表头 + 行 ---------- */
.cart-head,
.cart-row {
  display: grid;
  grid-template-columns: 46px 1fr 110px 140px 120px 80px;
  align-items: center;
  gap: 8px;
  background: #fff;
}

.cart-head {
  height: 44px;
  padding: 0 16px;
  border-radius: var(--tk-radius) var(--tk-radius) 0 0;
  border-bottom: 1px solid var(--tk-border);
  color: #666;
  font-size: 13px;
}

.cart-body {
  background: #fff;
  border-radius: 0 0 var(--tk-radius) var(--tk-radius);
  overflow: hidden;
}

.cart-row {
  padding: 14px 16px;
  border-top: 1px solid #f5f5f5;
}
.cart-row:first-child {
  border-top: none;
}
.cart-row:hover {
  background: #fffaf7;
}

.col-price,
.col-sub {
  text-align: center;
}
.col-sub {
  color: var(--tk-price);
  font-weight: 600;
}
.col-op {
  text-align: center;
}

.col-goods {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.thumb {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  border: 1px solid var(--tk-border);
  border-radius: 6px;
  overflow: hidden;
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.goods-info {
  min-width: 0;
}
.name-line {
  display: flex;
  align-items: center;
  gap: 8px;
}
.name {
  font-size: 14px;
  color: #333;
}
.sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--tk-text-sub);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---------- 结算条 ---------- */
.cart-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding: 16px 20px;
  background: #fff;
  border-radius: var(--tk-radius);
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
  position: sticky;
  bottom: 0;
}

.summary {
  display: flex;
  align-items: center;
  gap: 16px;
}

.count {
  font-size: 13px;
  color: var(--tk-text-sub);
}

.total {
  font-size: 16px;
  font-weight: 600;
  color: var(--tk-price);
}
.total em {
  font-style: normal;
  font-size: 13px;
}

.checkout {
  min-width: 120px;
}

/* 窄屏：把次要列收起来，只留商品、数量、操作 */
@media (max-width: 760px) {
  .cart-head {
    display: none;
  }
  .cart-row {
    grid-template-columns: 40px 1fr auto;
    row-gap: 8px;
  }
  .col-price,
  .col-sub {
    display: none;
  }
  .cart-bar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  .summary {
    justify-content: space-between;
  }
}
</style>
