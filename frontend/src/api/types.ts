// Tipos que reflejan el contrato JSON del backend (ver examples/README.md).
// Tenerlos aquí, en un único sitio, hace que TypeScript nos avise si el frontend y
// el backend dejan de cuadrar.

export type SearchPrecision = 'FAST' | 'BALANCED' | 'EXHAUSTIVE'

/** Cuerpo de POST /api/search. */
export interface SearchRequest {
  origin: string
  destination: string
  rangeStart: string // AAAA-MM-DD
  rangeEnd: string // AAAA-MM-DD
  baseDuration: number
  variability: number
  maxStops: number
  topN: number
  precision?: SearchPrecision
  maxPrice?: number // presupuesto máximo opcional (€); omitir = sin límite
}

/** Cada oferta del array que devuelve POST /api/search. */
export interface FlightOffer {
  airline: string
  price: number
  currency: string
  stops: number
  departDate: string
  returnDate: string
  departureTime: string | null // "HH:mm:ss" o null si la fuente no la da
  returnDepartureTime: string | null // hora de salida de la vuelta (null en SerpApi)
  stopovers: string[] // códigos IATA de los aeropuertos de escala
  bookingUrl: string
}

/** Forma del error 400 (ProblemDetail, RFC 7807). */
export interface ProblemDetail {
  title: string
  status: number
  detail: string
  instance: string
}
