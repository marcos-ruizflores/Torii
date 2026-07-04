import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

// Estilos base de Mantine (imprescindibles para que sus componentes se vean bien).
import '@mantine/core/styles.css'
import '@mantine/dates/styles.css'

import { MantineProvider } from '@mantine/core'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import App from './App.tsx'

// Cliente de TanStack Query: gestiona el estado de las llamadas a la API.
const queryClient = new QueryClient()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {/* MantineProvider habilita el sistema de temas/componentes de Mantine. */}
    <MantineProvider defaultColorScheme="auto">
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </MantineProvider>
  </StrictMode>,
)
