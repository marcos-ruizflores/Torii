import axios from 'axios'
import { http } from './http'
import type { FlightOffer, ProblemDetail, SearchRequest } from './types'

/** Llama a POST /api/search y devuelve las ofertas, o lanza un Error con el mensaje del backend. */
export async function searchOffers(request: SearchRequest): Promise<FlightOffer[]> {
  try {
    const { data } = await http.post<FlightOffer[]>('/api/search', request)
    return data
  } catch (err) {
    // Si el backend respondió un 400 con ProblemDetail, mostramos su "detail".
    if (axios.isAxiosError(err) && err.response?.data) {
      const problem = err.response.data as ProblemDetail
      throw new Error(problem.detail ?? 'Error en la búsqueda')
    }
    throw new Error('No se pudo contactar con el servidor de Torii')
  }
}
