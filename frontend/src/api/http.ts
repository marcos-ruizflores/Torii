import axios from 'axios'

// Shared HTTP client for the whole app. The backend URL comes from VITE_API_URL,
// set in the production build where frontend and API live on different origins.
// If it's not set, baseURL stays empty and "/api/..." goes to the same origin,
// which the Vite dev proxy forwards to the backend (see vite.config.ts).
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

// When logged in, every request carries the JWT so the backend can link searches
// to the user and protect /api/me/**.
http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
