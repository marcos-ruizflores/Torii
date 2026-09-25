import { http } from './http'

// Price history for a route (best price seen per day), served by the backend from
// the database (GET /api/price-history). It fills itself: every real search records
// its best price of the day. Early on the series will be almost empty, the chart
// says so and nudges the user to search.

export interface PricePoint {
  /** Day as YYYY-MM-DD. */
  date: string
  /** Best price seen that day. */
  price: number
  currency: string
}

/** Last `days` days of data for a route. */
export async function fetchPriceHistory(
  origin: string,
  destination: string,
  days: number,
): Promise<PricePoint[]> {
  const { data } = await http.get<PricePoint[]>('/api/price-history', {
    params: { origin, destination, days },
  })
  return data
}
