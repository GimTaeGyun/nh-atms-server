import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    }
  },
  server: {
    port: 3000,
    proxy: {
      // 배치관리(atms-batch, 8082) — 더 구체적인 경로를 먼저 매칭
      '/api/batch': {
        target: 'http://localhost:8082',
        changeOrigin: true
      },
      '/api':{
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
