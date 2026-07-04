import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthContext.tsx'
import App from './App.tsx'
import { Login } from './pages/Login.tsx'
import { NotFound } from './pages/NotFound.tsx'
import { Pricing } from './pages/Pricing.tsx'
import { SignUp } from './pages/SignUp.tsx'

// Estilos globales: Tailwind + theme de Untitled UI (colores, tipografía, tokens).
import '@/styles/globals.css'

// Cliente de TanStack Query: gestiona el estado de las llamadas a la API.
const queryClient = new QueryClient()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<App />} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<SignUp />} />
            <Route path="/planes" element={<Pricing />} />
            {/* Cualquier ruta desconocida cae en la página 404. */}
            <Route path="*" element={<NotFound />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  </StrictMode>,
)
