import axios from 'axios'

// Cliente HTTP compartido por toda la app. La URL del backend llega por
// VITE_API_URL (definida al hacer el build de producción, donde frontend y API
// viven en orígenes distintos). Si no está definida, baseURL queda vacío: las
// rutas "/api/..." salen al mismo origen, que en desarrollo el proxy de Vite
// redirige al backend (ver vite.config.ts).
export const http = axios.create({ baseURL: import.meta.env.VITE_API_URL ?? '' })

const TOKEN_KEY = 'torii.token'

export function saveToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

// Si hay sesión iniciada, todas las peticiones llevan el token JWT: así el
// backend puede asociar las búsquedas al usuario y proteger /api/me/**.
http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
