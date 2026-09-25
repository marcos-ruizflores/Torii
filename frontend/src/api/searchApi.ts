import axios from 'axios'
import { http } from './http'
import type { FlightOffer, ProblemDetail, SearchRequest } from './types'

/** Calls POST /api/search and returns the offers, or throws an Error with the backend message. */
export async function searchOffers(request: SearchRequest): Promise<FlightOffer[]> {
  try {
    const { data } = await http.post<FlightOffer[]>('/api/search', request)
    return data
  } catch (err) {
    // If the backend answered a 400 with a ProblemDetail, show its "detail".
    if (axios.isAxiosError(err) && err.response?.data) {
      const problem = err.response.data as ProblemDetail
      throw new Error(problem.detail ?? 'Error en la búsqueda')
    }
    throw new Error('No se pudo contactar con el servidor de Torii')
  }
}
