import path from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    // Alias "@/..." → src/. Lo usan los componentes de Untitled UI en sus imports.
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    // En desarrollo, el servidor de Vite (:5173) reenvía cualquier petición que
    // empiece por /api al backend de Spring Boot (:8080). Así el frontend habla con
    // "/api/search" como si fuera el mismo origen y nos ahorramos configurar CORS.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
