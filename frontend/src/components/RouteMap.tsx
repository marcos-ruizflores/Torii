import { geoEqualEarth, geoInterpolate } from 'd3-geo'
import { ComposableMap, Geographies, Geography, Line, Marker, type ProjectionFunction } from 'react-simple-maps'
import { token } from '@/utils/token'
import { lookupAirport, useAirports } from '../api/airports'
import { FlapText } from './board/FlapText'

interface Props {
  origin: string
  destination: string
}

// World TopoJSON (world-atlas countries-110m), served from our own public/ folder
// so the map doesn't depend on a third-party CDN being up.
const GEO_URL = '/geo/countries-110m.json'

/** Taller than wide on purpose: it sits next to the price chart and matches its height. */
const WIDTH = 600
const HEIGHT = 460
/** Room around the route for the airport labels. */
const PAD = 48
/** Cap for short hops (e.g. BCN–MAD), so the map never zooms into a blur of coastline. */
const MAX_SCALE = 2200

/**
 * Equal Earth projection fitted to the great-circle arc between the two airports,
 * so every route fills the panel: a long haul frames its arc, a short hop gets a
 * close-up (up to MAX_SCALE).
 */
function routeProjection(from: [number, number], to: [number, number]) {
  const arc = geoInterpolate(from, to)
  const coordinates = Array.from({ length: 33 }, (_, i) => arc(i / 32))
  const mid = arc(0.5)
  const projection = geoEqualEarth()
    .rotate([-mid[0], 0])
    .fitExtent(
      [
        [PAD, PAD],
        [WIDTH - PAD, HEIGHT - PAD],
      ],
      { type: 'LineString', coordinates },
    )
  if (projection.scale() > MAX_SCALE) {
    projection.scale(MAX_SCALE)
    const [x, y] = projection(mid) ?? [WIDTH / 2, HEIGHT / 2]
    const [tx, ty] = projection.translate()
    projection.translate([tx + WIDTH / 2 - x, ty + HEIGHT / 2 - y])
  }
  // react-simple-maps uses a function passed as `projection` as the projection itself.
  return projection as unknown as ProjectionFunction
}

/** Board panel header: name on the left, the route in flaps next to it. */
function PanelHeader({ origin, destination }: Props) {
  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 border-b border-secondary px-5 py-4">
      <h2 className="font-display text-lg font-semibold tracking-wide text-primary uppercase">Ruta</h2>
      <FlapText text={`${origin} → ${destination}`} className="text-lg" />
    </div>
  )
}

/**
 * Map of the current search's route, framed on the two airports.
 *
 * Coordinates come from the airport list (loaded on demand). If a code isn't in
 * there it shows a notice instead of breaking.
 */
export function RouteMap({ origin, destination }: Props) {
  const airports = useAirports()
  const from = lookupAirport(airports.data, origin)
  const to = lookupAirport(airports.data, destination)

  // Same size as the finished panel while the list loads, so nothing jumps.
  if (airports.isPending) {
    return (
      <section className="flex min-h-80 flex-col overflow-hidden rounded-xl bg-secondary ring-1 ring-secondary ring-inset">
        <PanelHeader origin={origin} destination={destination} />
      </section>
    )
  }

  if (!from || !to) {
    const desconocido = !from ? origin : destination
    return (
      <section className="overflow-hidden rounded-xl bg-secondary ring-1 ring-secondary ring-inset">
        <PanelHeader origin={origin} destination={destination} />
        <div className="px-5 py-5 text-sm">
          <p className="font-semibold text-primary">Mapa no disponible</p>
          <p className="mt-1 text-tertiary">
            No tengo las coordenadas del aeropuerto «{desconocido}». El mapa conoce los aeropuertos con vuelos
            regulares; la búsqueda funciona igual.
          </p>
        </div>
      </section>
    )
  }

  const ink = token('--color-neutral-50')
  const signal = token('--color-brand-600')
  const labelFont = token('--font-display')

  return (
    <section className="flex flex-col overflow-hidden rounded-xl bg-secondary ring-1 ring-secondary ring-inset">
      <PanelHeader origin={origin} destination={destination} />
      <p className="px-5 pt-4 text-sm text-secondary">
        {from.city} → {to.city}
      </p>
      <div className="flex flex-1 items-center px-2 pb-2">
        <ComposableMap
          projection={routeProjection(from.coordinates, to.coordinates)}
          width={WIDTH}
          height={HEIGHT}
          style={{ width: '100%', height: 'auto' }}
          aria-label={`Mapa de la ruta ${from.city} a ${to.city}`}
        >
          <Geographies geography={GEO_URL}>
            {({ geographies }) =>
              geographies.map((geo) => (
                <Geography
                  key={geo.rsmKey}
                  geography={geo}
                  fill={token('--color-neutral-800')}
                  stroke={token('--color-neutral-950')}
                  strokeWidth={0.5}
                  style={{
                    default: { outline: 'none' },
                    hover: { fill: token('--color-neutral-700'), outline: 'none' },
                    pressed: { outline: 'none' },
                  }}
                />
              ))
            }
          </Geographies>

          {/* Route line between the two airports. */}
          <Line from={from.coordinates} to={to.coordinates} stroke={signal} strokeWidth={2} strokeDasharray="6 5" strokeLinecap="round" />

          {/* Origin marker (ink). */}
          <Marker coordinates={from.coordinates}>
            <rect x={-5} y={-5} width={10} height={10} fill={ink} />
            <text textAnchor="middle" y={-14} fontSize={20} fontWeight={600} fontFamily={labelFont} fill={ink}>
              {origin}
            </text>
          </Marker>

          {/* Destination marker (signal). */}
          <Marker coordinates={to.coordinates}>
            <rect x={-5} y={-5} width={10} height={10} fill={signal} />
            <text textAnchor="middle" y={-14} fontSize={20} fontWeight={600} fontFamily={labelFont} fill={signal}>
              {destination}
            </text>
          </Marker>
        </ComposableMap>
      </div>
    </section>
  )
}
