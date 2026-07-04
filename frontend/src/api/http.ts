import axios from 'axios'

// Cliente HTTP compartido por toda la app. La URL del backend llega por
// VITE_API_URL (definida al hacer el build de producción, donde frontend y API
// viven en orígenes distintos). Si no está definida, baseURL queda vacío: las
// rutas "/api/..." salen al mismo origen, que en desarrollo el proxy de Vite
// redirige al backend (ver vite.config.ts).
export const http = axios.create({ baseURL: import.meta.env.VITE_API_URL ?? '' })
