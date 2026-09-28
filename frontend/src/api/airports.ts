import { useQuery } from '@tanstack/react-query'

// Airport lookup: IATA code -> city, name, country and coordinates.
//
// The backend doesn't return coordinates (FlightOffer doesn't have them), so the map
// and the airport field resolve them here. The data is ~3,200 large and medium
// airports with scheduled service, generated from OurAirports by
// scripts/build-airports.mjs (`npm run airports`). It's ~90 kB gzipped, so it's
// loaded on demand: when the map shows up or the user focuses an airport field.
//
// Coordinates are [longitude, latitude], the order GeoJSON / react-simple-maps use.

export interface Airport {
  iata: string
  /** City in Spanish when we have the name, otherwise as OurAirports spells it. */
  city: string
  /** Official airport name (in English, as published). */
  name: string
  /** ISO 3166 country code. */
  country: string
  coordinates: [number, number]
  /** Large hub: ranks first in suggestions. */
  large: boolean
}

type Row = [string, string, string, string, number, number, number]

export type AirportIndex = Map<string, Airport>

let index: Promise<AirportIndex> | null = null

/** Loads the airport list once; later calls reuse the same promise. */
export function loadAirports(): Promise<AirportIndex> {
  index ??= import('../data/airports.json').then(({ default: rows }) => {
    const map: AirportIndex = new Map()
    for (const [iata, city, name, country, lon, lat, large] of rows as Row[]) {
      map.set(iata, { iata, city, name, country, coordinates: [lon, lat], large: large === 1 })
    }
    return map
  })
  return index
}

/** The airport list as a query, so components re-render when it arrives. */
export function useAirports(enabled = true) {
  return useQuery({ queryKey: ['airports'], queryFn: loadAirports, staleTime: Infinity, gcTime: Infinity, enabled })
}

export function lookupAirport(airports: AirportIndex | undefined, iata: string): Airport | undefined {
  return airports?.get(iata.toUpperCase())
}

const countries = typeof Intl.DisplayNames === 'function' ? new Intl.DisplayNames(['es'], { type: 'region' }) : null

/** "España", "Japón"... falls back to the ISO code. */
export function countryName(code: string): string {
  try {
    return countries?.of(code) ?? code
  } catch {
    return code
  }
}

/** Lowercase without accents, so "malaga" finds "Málaga". */
function fold(text: string): string {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
}

/**
 * Suggestions for what the user typed: exact code first, then cities and airport
 * names that start with the text, then ones that contain it. Ties go to big hubs,
 * to the spelling the user actually typed ("mála" -> Málaga before Malabo) and to
 * Spanish airports, where most searches start.
 */
export function searchAirports(airports: AirportIndex, query: string, limit = 8): Airport[] {
  const raw = query.trim().toLowerCase()
  const q = fold(raw)
  if (!q) return []
  const scored: { airport: Airport; score: number }[] = []
  for (const airport of airports.values()) {
    const code = airport.iata.toLowerCase()
    const city = fold(airport.city)
    const name = fold(airport.name)
    let score = -1
    if (code === q) score = 100
    else if (code.startsWith(q)) score = 60
    else if (city.startsWith(q)) score = 50
    else if (name.startsWith(q) || city.includes(` ${q}`) || name.includes(` ${q}`)) score = 30
    else if (q.length >= 3 && (city.includes(q) || name.includes(q))) score = 10
    if (score < 0) continue
    if (airport.large) score += 5
    if (airport.city.toLowerCase().startsWith(raw)) score += 3
    if (airport.country === 'ES') score += 2
    scored.push({ airport, score })
  }
  return scored
    .sort((a, b) => b.score - a.score || a.airport.city.localeCompare(b.airport.city, 'es'))
    .slice(0, limit)
    .map((s) => s.airport)
}
