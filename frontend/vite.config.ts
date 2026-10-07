import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // 配了别名，后面写 import 就能用 '@/api/http' 而不是一堆 ../../../
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 前端请求 /api/xxx → 转发到网关 8080
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
        // 不需要 rewrite：网关的路由本身就是以 /api 开头的
      }
    }
  }
})
