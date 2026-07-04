import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
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
