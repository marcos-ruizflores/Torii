import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router'
import App from './App.tsx'
import { NotFound } from './pages/NotFound.tsx'

// Estilos globales: Tailwind + theme de Untitled UI (colores, tipografía, tokens).
import '@/styles/globals.css'

// Cliente de TanStack Query: gestiona el estado de las llamadas a la API.
const queryClient = new QueryClient()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<App />} />
          {/* Cualquier ruta desconocida cae en la página 404. */}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
