import axios from 'axios'
import { http } from './http'
import type { AuthResponse, ProblemDetail, QuotaUsage, SavedSearch, User } from './types'

/** Pulls the useful message out of a backend error (ProblemDetail or plain text). */
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

/** Profile of the stored token's owner. Fails if the token has expired. */
export async function fetchMe(): Promise<User> {
  const { data } = await http.get<User>('/api/me')
  return data
}

/** Latest searches of the logged in user (the backend allows up to 50). */
export async function fetchMySearches(limit = 10): Promise<SavedSearch[]> {
  const { data } = await http.get<SavedSearch[]>('/api/me/searches', { params: { limit } })
  return data
}

/** This month's quota: how many lookups the user has used and their limit. */
export async function fetchMyUsage(): Promise<QuotaUsage> {
  const { data } = await http.get<QuotaUsage>('/api/me/usage')
  return data
}

/** Changes the account plan (no payments yet). Returns the updated profile. */
export async function changePlan(plan: User['plan']): Promise<User> {
  try {
    const { data } = await http.post<User>('/api/me/plan', { plan })
    return data
  } catch (err) {
    throw new Error(messageFrom(err, 'No se pudo cambiar el plan'))
  }
}
