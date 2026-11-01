import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  // ================= 代理配置：多后端服务 =================
  server: {
    port: 5173, // 指定前端项目的运行端口（Vite 默认就是 5173，可以按需修改）
    open: true, // 运行 npm run dev 时自动在浏览器打开页面
    proxy: {
      // 用户端 API → user8011 服务（端口 8011，context-path /user8011）
      '/api': {
        target: 'http://localhost:8011/user8011',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      // 管理端 API → admin8012 服务（端口 8012，context-path /admin8012）
      '/admin-api': {
        target: 'http://localhost:8012/admin8012',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/admin-api/, '')
      }
    }
  }
})