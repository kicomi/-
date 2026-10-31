import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 学生端 H5，开发端口 5174，接口统一走 /api 代理到 Spring Boot
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    host: '127.0.0.1',
    proxy: {
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/uploads': { target: 'http://127.0.0.1:8080', changeOrigin: true }
    }
  },
  build: { outDir: 'dist' }
})
