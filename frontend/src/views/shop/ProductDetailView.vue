<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ShopHeader from '@/components/ShopHeader.vue'
import { getProduct } from '@/api/product'
import { addItem } from '@/api/cart'
import { createOrder, newRequestId } from '@/api/order'
import { errorMessage, isApiError } from '@/api/http'
import { useCartStore } from '@/stores/cart'
import type { Product } from '@/types/api'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const product = ref<Product | null>(null)
const loading = ref(false)
/** 商品已下架或已删除：这是正常业务分支，不是错误 */
const notFound = ref(false)
const quantity = ref(1)
const adding = ref(false)
const buying = ref(false)

const soldOut = computed(() => (product.value?.stock ?? 0) <= 0)

async function load() {
  loading.value = true
  notFound.value = false

  try {
    product.value = await getProduct(Number(route.params.id))
  } catch (error) {
    // 404 表示商品下架/删除，用专门的状态展示，不弹错误提示
    if (isApiError(error) && error.status === 404) {
      notFound.value = true
    } else {
      ElMessage.error(errorMessage(error, '商品加载失败'))
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function handleAddToCart() {
  if (!product.value) return

  adding.value = true
  try {
    await addItem(product.value.id, quantity.value)
    await cartStore.refresh()
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(errorMessage(error, '加入购物车失败'))
  } finally {
    adding.value = false
  }
}

/**
 * 立即购买：不走购物车，直接下成一笔订单。
 * 订单价格由后端从商品服务取真实值，前端只传商品 ID 和数量。
 */
async function handleBuyNow() {
  if (!product.value) return

  buying.value = true
  try {
    const orderId = await createOrder(
      { items: [{ productId: product.value.id, quantity: quantity.value }] },
      newRequestId()
    )
    ElMessage.success('下单成功')
    await router.push(`/orders/${orderId}`)
  } catch (error) {
    // 库存不足是 409、商品已下架是 404，后端都给了可读提示
    ElMessage.error(errorMessage(error, '下单失败'))
  } finally {
    buying.value = false
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
      <el-result
        v-if="notFound"
        icon="warning"
        title="商品已下架"
        sub-title="该商品可能已被下架或删除，看看其他商品吧"
      >
        <template #extra>
          <el-button type="primary" @click="router.push('/products')">
            返回商品列表
          </el-button>
        </template>
      </el-result>

      <div v-else-if="product" class="detail">
        <!-- 左：商品图 -->
        <div class="gallery">
          <div class="thumb">
            <img
              v-if="product.mainImage"
              :src="product.mainImage"
              :alt="product.name"
            />
            <span v-else class="tk-thumb-empty">暂无图片</span>
          </div>
        </div>

        <!-- 右：商品信息 + 下单区 -->
        <div class="info">
          <h1 class="name">{{ product.name }}</h1>
          <p class="subtitle">{{ product.subtitle || '—' }}</p>

          <div class="price-box">
            <div class="price-row">
              <span class="label">价格</span>
              <span class="price"><em>¥</em>{{ formatPrice(product.price) }}</span>
              <el-tag v-if="soldOut" type="info">售罄</el-tag>
            </div>
            <div class="price-row sub">
              <span class="label">库存</span>
              <span class="stock">{{ product.stock }} 件</span>
            </div>
          </div>

          <div class="buy-row">
            <span class="label">数量</span>
            <el-input-number
              v-model="quantity"
              :min="1"
              :max="Math.max(product.stock, 1)"
              :disabled="soldOut"
            />
          </div>

          <div class="buy-actions">
            <el-button
              class="btn-cart"
              size="large"
              :disabled="soldOut"
              :loading="adding"
              @click="handleAddToCart"
            >
              加入购物车
            </el-button>

            <el-button
              class="tk-btn-main btn-buy"
              size="large"
              :disabled="soldOut"
              :loading="buying"
              @click="handleBuyNow"
            >
              {{ soldOut ? '已售罄' : '立即购买' }}
            </el-button>
          </div>

          <ul class="promise">
            <li>正品保障</li>
            <li>七天无理由退换</li>
            <li>超时未支付自动关闭并归还库存</li>
          </ul>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.detail {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 32px;
  padding: 24px;
  background: #fff;
  border-radius: var(--tk-radius);
}

/* ---------- 左图 ---------- */
.gallery {
  align-self: start;
}
.thumb {
  width: 100%;
  aspect-ratio: 1 / 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border: 1px solid var(--tk-border);
  border-radius: var(--tk-radius);
  overflow: hidden;
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ---------- 右信息 ---------- */
.info {
  min-width: 0;
}

.name {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
  color: #222;
}

.subtitle {
  margin: 0 0 20px;
  font-size: 14px;
  color: var(--tk-text-sub);
}

.price-box {
  padding: 16px 20px;
  margin-bottom: 24px;
  border-radius: var(--tk-radius);
  background: linear-gradient(90deg, #fff3ea, #ffe8dc);
}

.price-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.price-row.sub {
  margin-top: 8px;
}

.label {
  width: 40px;
  font-size: 13px;
  color: var(--tk-text-sub);
}

.price {
  color: var(--tk-price);
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
}
.price em {
  font-style: normal;
  font-size: 16px;
  margin-right: 2px;
}

.stock {
  font-size: 13px;
  color: #666;
}

.buy-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.buy-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-cart {
  min-width: 140px;
  height: 46px;
  color: var(--tk-brand);
  border-color: var(--tk-brand);
  background: #fff7f2;
}
.btn-cart:hover {
  color: #fff;
  background: var(--tk-brand);
  border-color: var(--tk-brand);
}

.btn-buy {
  min-width: 160px;
  height: 46px;
  font-size: 16px;
}

.promise {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin: 24px 0 0;
  padding: 16px 0 0;
  border-top: 1px dashed var(--tk-border);
  list-style: none;
  font-size: 12px;
  color: var(--tk-text-sub);
}
.promise li::before {
  content: '✓';
  margin-right: 4px;
  color: var(--tk-brand);
}

@media (max-width: 860px) {
  .detail {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 16px;
  }
  .name {
    font-size: 20px;
  }
}
</style>
