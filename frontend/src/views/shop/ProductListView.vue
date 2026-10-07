<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ShopHeader from '@/components/ShopHeader.vue'
import { listProducts } from '@/api/product'
import { addItem } from '@/api/cart'
import { errorMessage } from '@/api/http'
import { useCartStore } from '@/stores/cart'
import type { Product } from '@/types/api'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()

const products = ref<Product[]>([])
const loading = ref(false)
/** 顶栏搜索框把关键字放在地址栏里，这里读出来做前端过滤 */
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')

const visibleProducts = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return products.value
  return products.value.filter(
    (p) =>
      p.name.toLowerCase().includes(kw) ||
      (p.subtitle ?? '').toLowerCase().includes(kw)
  )
})

async function load() {
  loading.value = true
  try {
    products.value = await listProducts()
  } catch (error) {
    ElMessage.error(errorMessage(error, '商品加载失败'))
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await load()
  cartStore.refresh()
})

function goDetail(id: number) {
  router.push(`/products/${id}`)
}

async function handleAddToCart(item: Product) {
  try {
    await addItem(item.id, 1)
    await cartStore.refresh()
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(errorMessage(error, '加入购物车失败'))
  }
}

/** 金额统一保留两位小数展示。注意：只用于显示，不要拿来算加减 */
function formatPrice(price: number) {
  return Number(price).toFixed(2)
}
</script>

<template>
  <div class="tk-page">
    <ShopHeader />

    <!-- 顶部横幅：给首页一个"平台感"的视觉重心 -->
    <section class="hero">
      <div class="hero-inner">
        <p class="hero-sub">TAKEJ · 品质好物</p>
        <h1 class="hero-title">一站购齐你想要的一切</h1>
        <p class="hero-desc">正品保障 · 七天无理由 · 满额包邮</p>
      </div>
    </section>

    <main v-loading="loading" class="tk-container">
      <div class="tk-title">
        全部商品
        <span class="count">共 {{ visibleProducts.length }} 件</span>
        <span v-if="keyword.trim()" class="kw">
          「{{ keyword }}」
          <el-button link type="primary" @click="keyword = ''">清空</el-button>
        </span>
      </div>

      <el-empty
        v-if="!loading && visibleProducts.length === 0"
        :description="keyword.trim() ? '没有匹配的商品' : '暂无在售商品'"
      />

      <div v-else class="grid">
        <div
          v-for="item in visibleProducts"
          :key="item.id"
          class="card"
          @click="goDetail(item.id)"
        >
          <div class="thumb">
            <img v-if="item.mainImage" :src="item.mainImage" :alt="item.name" />
            <span v-else class="tk-thumb-empty">暂无图片</span>
            <el-tag v-if="item.stock <= 0" type="info" size="small" class="sold">
              售罄
            </el-tag>
          </div>

          <div class="body">
            <h3 class="name">{{ item.name }}</h3>
            <p class="subtitle">{{ item.subtitle || '—' }}</p>

            <div class="foot">
              <span class="price">
                <em>¥</em>{{ formatPrice(item.price) }}
              </span>
              <span class="stock">库存 {{ item.stock }}</span>
            </div>

            <el-button
              class="add-btn"
              size="small"
              :disabled="item.stock <= 0"
              @click.stop="handleAddToCart(item)"
            >
              加入购物车
            </el-button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* ---------- 横幅 ---------- */
.hero {
  background: linear-gradient(120deg, #ff7a2f 0%, #ff5000 45%, #ff2b00 100%);
  color: #fff;
  padding: 44px 16px 52px;
}
.hero-inner {
  max-width: var(--tk-container);
  margin: 0 auto;
}
.hero-sub {
  margin: 0 0 8px;
  font-size: 13px;
  letter-spacing: 4px;
  opacity: 0.85;
}
.hero-title {
  margin: 0 0 10px;
  font-size: 32px;
  font-weight: 800;
  letter-spacing: 1px;
}
.hero-desc {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

/* ---------- 列表 ---------- */
.count {
  font-size: 13px;
  font-weight: 400;
  color: var(--tk-text-sub);
}
.kw {
  font-size: 13px;
  font-weight: 400;
  color: var(--tk-brand);
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 16px;
}

.card {
  background: #fff;
  border: 1px solid var(--tk-border);
  border-radius: var(--tk-radius);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
/* hover 轻微上浮 + 投影：这是大平台商品卡的标志性反馈 */
.card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  border-color: transparent;
}

.thumb {
  position: relative;
  height: 190px;
  background: #fafafa;
  overflow: hidden;
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}
.card:hover .thumb img {
  transform: scale(1.05);
}
.sold {
  position: absolute;
  top: 8px;
  left: 8px;
}

.body {
  padding: 12px;
}

.name {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  /* 最多两行，超出的打点，卡片高度才不会参差 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.subtitle {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--tk-text-sub);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}

.price {
  color: var(--tk-price);
  font-size: 20px;
  font-weight: 700;
}
.price em {
  font-style: normal;
  font-size: 13px;
  margin-right: 1px;
}

.stock {
  font-size: 12px;
  color: var(--tk-text-sub);
}

.add-btn {
  width: 100%;
}

@media (max-width: 640px) {
  .hero-title {
    font-size: 24px;
  }
}
</style>
