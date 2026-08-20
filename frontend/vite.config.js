import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const gatewayTarget = process.env.VITE_GATEWAY_TARGET || 'http://localhost:8088'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: gatewayTarget,
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.removeHeader('origin')
          })
        }
      }
    }
  }
})
