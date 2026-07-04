import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import * as authApi from '../api/authApi'
import { clearToken, getToken, saveToken } from '../api/http'
import type { User } from '../api/types'

/**
 * Estado de sesión de toda la app. El token JWT vive en localStorage (sobrevive a
 * recargas); el perfil se restaura al arrancar pidiendo /api/me con ese token.
 */
interface AuthState {
  /** Usuario con sesión iniciada, o null si es anónimo. */
  user: User | null
  login: (email: string, password: string) => Promise<void>
  signup: (name: string, email: string, password: string) => Promise<void>
  logout: () => void
  /** Actualiza el perfil en memoria (p. ej. tras cambiar de plan). */
  updateUser: (user: User) => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)

  // Al cargar la app: si hay token guardado, intentamos restaurar la sesión.
  // Si el token caducó o la cuenta ya no existe, se limpia y se sigue anónimo.
  useEffect(() => {
    if (!getToken()) return
    authApi
      .fetchMe()
      .then(setUser)
      .catch(() => clearToken())
  }, [])

  async function login(email: string, password: string) {
    const { token, user } = await authApi.login(email, password)
    saveToken(token)
    setUser(user)
  }

  async function signup(name: string, email: string, password: string) {
    const { token, user } = await authApi.signup(name, email, password)
    saveToken(token)
    setUser(user)
  }

  function logout() {
    clearToken()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, signup, logout, updateUser: setUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}
