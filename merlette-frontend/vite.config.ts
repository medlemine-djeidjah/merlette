import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.MERLETTE_API_TARGET || 'http://localhost:8080'
  const proxy = {
    '/api': { target, changeOrigin: true, proxyTimeout: 15000, timeout: 20000 },
    '/uploads': { target, changeOrigin: true, proxyTimeout: 15000, timeout: 20000 },
  }
  return {
    plugins: [react(), tailwindcss()],
    server: { port: 5173, strictPort: true, proxy },
    preview: { proxy },
  }
})
