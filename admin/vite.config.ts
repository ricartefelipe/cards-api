import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_API || 'http://54.94.163.136:8083',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '/auth': {
        target: process.env.VITE_PROXY_AUTH || 'http://54.94.163.136:8181',
        changeOrigin: true,
        rewrite: (path) =>
          path.replace(/^\/auth/, '/realms/quarkus/protocol/openid-connect'),
      },
    },
  },
})
