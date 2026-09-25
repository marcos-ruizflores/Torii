import { useMutation } from '@tanstack/react-query'
import { searchOffers } from '../api/searchApi'
import type { FlightOffer, SearchRequest } from '../api/types'

/**
 * Wraps the search as a TanStack Query mutation.
 *
 * useMutation instead of useQuery because the search is triggered by the user
 * clicking "Search", not automatically on load. We still get isPending / isError /
 * data for free, which the UI uses for the spinner, the error and the results.
 */
export function useSearch() {
  return useMutation<FlightOffer[], Error, SearchRequest>({
    mutationFn: searchOffers,
  })
}
