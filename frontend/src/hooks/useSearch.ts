import { useMutation } from '@tanstack/react-query'
import { searchOffers } from '../api/searchApi'
import type { FlightOffer, SearchRequest } from '../api/types'

/**
 * Encapsula la búsqueda como una "mutación" de TanStack Query.
 *
 * Usamos useMutation (y no useQuery) porque la búsqueda se dispara por una acción
 * del usuario —pulsar "Buscar"— y no automáticamente al cargar. A cambio nos da
 * gratis los estados isPending / isError / data, que la UI usa para mostrar el
 * spinner, el error o la tabla.
 */
export function useSearch() {
  return useMutation<FlightOffer[], Error, SearchRequest>({
    mutationFn: searchOffers,
  })
}
