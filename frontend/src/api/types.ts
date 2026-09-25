// Types mirroring the backend JSON contract (see examples/README.md). Keeping them
// in one place means TypeScript complains if frontend and backend drift apart.

export type SearchPrecision = 'FAST' | 'BALANCED' | 'EXHAUSTIVE'

/** Body of POST /api/search. */
export interface SearchRequest {
  origin: string
  destination: string
  rangeStart: string // YYYY-MM-DD
  rangeEnd: string // YYYY-MM-DD
  baseDuration: number
  variability: number
  maxStops: number
  topN: number
  precision?: SearchPrecision
  maxPrice?: number // optional budget in EUR, leave out for no limit
}

/** One offer from the array returned by POST /api/search. */
export interface FlightOffer {
  airline: string
  price: number
  currency: string
  stops: number
  departDate: string
  returnDate: string
  departureTime: string | null // "HH:mm:ss", or null if the source doesn't provide it
  returnDepartureTime: string | null // return leg departure time (null with SerpApi)
  stopovers: string[] // IATA codes of the stopover airports
  bookingUrl: string
}

/** Shape of a 400 error (ProblemDetail, RFC 7807). */
export interface ProblemDetail {
  title: string
  status: number
  detail: string
  instance: string
}

/** Public profile of the logged in user (returned by login/signup and /api/me). */
export interface User {
  id: number
  name: string
  email: string
  plan: 'FREE' | 'PRO' | 'BUSINESS'
}

/** Response of POST /api/auth/login and /api/auth/signup. */
export interface AuthResponse {
  token: string
  user: User
}

/** Monthly quota from GET /api/me/usage. A null limit means unlimited. */
export interface QuotaUsage {
  plan: string
  limit: number | null
  used: number
  month: string
}

/** A saved search from GET /api/me/searches, can be repeated with one click. */
export interface SavedSearch {
  id: number
  origin: string
  destination: string
  rangeStart: string
  rangeEnd: string
  baseDuration: number
  variability: number
  maxStops: number
  topN: number
  precision: SearchPrecision
  maxPrice: number | null
  createdAt: string
}
