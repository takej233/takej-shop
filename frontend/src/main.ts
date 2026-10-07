import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 全局主题必须放在 element-plus 之后，才能覆盖它的默认色
import './styles/global.css'
import router from './router'
import App from './App.vue'

const app = createApp(App)

// 三个插件都要装：状态管理、路由、UI 组件库
app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
