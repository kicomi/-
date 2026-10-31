import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import './styles/main.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 图标全量注册，页面里直接写 <el-icon><User /></el-icon> 就行
for (const [name, comp] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, comp)
}

app.use(ElementPlus, { locale: zhCn })
app.use(router)
app.mount('#app')
