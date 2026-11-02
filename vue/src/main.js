import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus, { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'
import "@/assets/global.css"
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './assets/css-vars.css'

// 全局注册弹窗一键复制辅助函数
if (typeof window !== 'undefined') {
  window.__copyTempPassword = (btn, text) => {
    if (!text) return
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).catch(() => {})
    } else {
      const input = document.createElement('textarea')
      input.value = text
      input.style.position = 'fixed'
      input.style.opacity = '0'
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
    }
    ElMessage.success('密码已成功复制到剪贴板！')
    if (btn) {
      const orig = btn.innerHTML
      btn.innerHTML = '<span style="display:inline-flex;align-items:center;gap:4px;">已复制 ✓</span>'
      btn.style.backgroundColor = '#10b981'
      setTimeout(() => {
        btn.innerHTML = orig
        btn.style.backgroundColor = 'var(--el-color-primary, #1677ff)'
      }, 2000)
    }
  }
}

import { MotionPlugin } from '@vueuse/motion'

const app = createApp(App)

app.use(MotionPlugin)
app.use(router)
app.use(ElementPlus, {
    locale: zhCn,
})
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
app.mount('#app')
