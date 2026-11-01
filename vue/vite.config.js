import { fileURLToPath, URL } from 'node:url'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import ElementPlus from 'unplugin-element-plus/vite'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    ElementPlus({
      useSource: true,
    }),
    AutoImport({
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })],
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })],
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  optimizeDeps: {
    include: [
      'vue',
      'vue-router',
      'axios',
      'dayjs',
      'echarts',
      '@vueuse/core',
      '@element-plus/icons-vue',
      'element-plus',
      'element-plus/es',
      'markdown-it',
      'openai',
      '@wangeditor/editor',
      '@wangeditor/editor-for-vue'
    ]
  },
  server: {
    warmup: {
      clientFiles: [
        './src/main.js',
        './src/App.vue',
        './src/router/index.js',
        './src/views/Login.vue',
        './src/views/MainFrame.vue',
        './src/views/Home.vue',
        './src/views/User.vue',
        './src/views/Admin.vue',
        './src/views/Bill.vue',
        './src/views/Notebook.vue',
        './src/views/AIAnalysis.vue',
        './src/views/Personal.vue'
      ]
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/assets/index.scss" as *;`,
        api: 'modern-compiler'
      }
    }
  }
})
