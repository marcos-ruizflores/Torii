import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthContext.tsx'
import App from './App.tsx'
import { Login } from './pages/Login.tsx'
import { MySearches } from './pages/MySearches.tsx'
import { NotFound } from './pages/NotFound.tsx'
import { Pricing } from './pages/Pricing.tsx'
import { SignUp } from './pages/SignUp.tsx'

// Global styles: Tailwind + Untitled UI theme (colors, typography, tokens).
import '@/styles/globals.css'

// TanStack Query client, handles the state of API calls.
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
            <Route path="/mis-busquedas" element={<MySearches />} />
            {/* Any unknown route falls through to the 404 page. */}
            <Route path="*" element={<NotFound />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  </StrictMode>,
)
