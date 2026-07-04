import axios from 'axios'
import { http } from './http'
import type { AuthResponse, ProblemDetail, SavedSearch, User } from './types'

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

/** Las últimas búsquedas del usuario autenticado. */
export async function fetchMySearches(): Promise<SavedSearch[]> {
  const { data } = await http.get<SavedSearch[]>('/api/me/searches')
  return data
}
