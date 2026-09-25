import path from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    // "@/..." alias -> src/. The Untitled UI components use it in their imports.
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    // In dev the Vite server (:5173) forwards anything under /api to the Spring
    // Boot backend (:8080). The frontend calls "/api/search" as if it were the same
    // origin, so no CORS setup is needed locally.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
