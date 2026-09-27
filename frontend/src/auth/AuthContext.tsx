import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import * as authApi from '../api/authApi'
import { clearToken, getToken, saveToken } from '../api/http'
import type { User } from '../api/types'

/**
 * App-wide session state. The JWT lives in localStorage so it survives reloads, and
 * the profile is restored on startup by calling /api/me with it.
 */
interface AuthState {
  /** Logged in user, or null when anonymous. */
  user: User | null
  /** True while a stored token is being checked on startup; `user` isn't final yet. */
  restoring: boolean
  login: (email: string, password: string) => Promise<void>
  signup: (name: string, email: string, password: string) => Promise<void>
  /** Redeems a reset link and logs in with the new password. */
  resetPassword: (token: string, password: string) => Promise<void>
  logout: () => void
  /** Updates the in-memory profile (e.g. after a plan change). */
  updateUser: (user: User) => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [restoring, setRestoring] = useState(() => !!getToken())

  // On load, try to restore the session if there's a stored token. If it expired
  // or the account is gone, clear it and carry on as anonymous.
  useEffect(() => {
    if (!getToken()) return
    authApi
      .fetchMe()
      .then(setUser)
      .catch(() => clearToken())
      .finally(() => setRestoring(false))
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

  async function resetPassword(token: string, password: string) {
    const { token: jwt, user } = await authApi.resetPassword(token, password)
    saveToken(jwt)
    setUser(user)
  }

  function logout() {
    clearToken()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, restoring, login, signup, resetPassword, logout, updateUser: setUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}
