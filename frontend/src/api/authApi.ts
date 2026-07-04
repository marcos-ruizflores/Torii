import axios from 'axios'
import { http } from './http'
import type { AuthResponse, ProblemDetail, QuotaUsage, SavedSearch, User } from './types'

/** Extrae el mensaje útil de un error del backend (ProblemDetail o texto plano). */
function messageFrom(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err) && err.response?.data) {
    const problem = err.response.data as ProblemDetail
    return problem.detail ?? fallback
  }
  return fallback
}

export async function signup(name: string, email: string, password: string): Promise<AuthResponse> {
  try {
    const { data } = await http.post<AuthResponse>('/api/auth/signup', { name, email, password })
    return data
  } catch (err) {
    throw new Error(messageFrom(err, 'No se pudo crear la cuenta'))
  }
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  try {
    const { data } = await http.post<AuthResponse>('/api/auth/login', { email, password })
    return data
  } catch (err) {
    throw new Error(messageFrom(err, 'Email o contraseña incorrectos'))
  }
}

/** Perfil del dueño del token guardado; falla si el token caducó. */
export async function fetchMe(): Promise<User> {
  const { data } = await http.get<User>('/api/me')
  return data
}

/** Las últimas búsquedas del usuario autenticado (el backend admite hasta 50). */
export async function fetchMySearches(limit = 10): Promise<SavedSearch[]> {
  const { data } = await http.get<SavedSearch[]>('/api/me/searches', { params: { limit } })
  return data
}

/** Cuota del mes: cuántas consultas lleva gastadas el usuario y cuál es su límite. */
export async function fetchMyUsage(): Promise<QuotaUsage> {
  const { data } = await http.get<QuotaUsage>('/api/me/usage')
  return data
}

/** Cambia el plan de la cuenta (sin pagos todavía). Devuelve el perfil actualizado. */
export async function changePlan(plan: User['plan']): Promise<User> {
  try {
    const { data } = await http.post<User>('/api/me/plan', { plan })
    return data
  } catch (err) {
    throw new Error(messageFrom(err, 'No se pudo cambiar el plan'))
  }
}
