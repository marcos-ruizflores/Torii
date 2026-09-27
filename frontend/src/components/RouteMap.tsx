import { ComposableMap, Geographies, Geography, Line, Marker } from 'react-simple-maps'
import { token } from '@/utils/token'
import { lookupAirport } from '../api/airports'
import { FlapText } from './board/FlapText'

interface Props {
  origin: string
  destination: string
}

// World TopoJSON from a CDN (the usual react-simple-maps setup).
const GEO_URL = 'https://cdn.jsdelivr.net/npm/world-atlas@2/countries-110m.json'

const WIDTH = 600
const HEIGHT = 380
/** Scale that fits the whole world in WIDTH with the Equal Earth projection. */
const WORLD_SCALE = 115

/**
 * Centre and zoom that frame the route: short hops get a close-up, long hauls stay
 * close to the world view.
 */
function frame(from: [number, number], to: [number, number]) {
  const lon = (from[0] + to[0]) / 2
  const lat = (from[1] + to[1]) / 2
  const span = Math.hypot(from[0] - to[0], from[1] - to[1])
  const scale = Math.min(1600, Math.max(WORLD_SCALE, (WORLD_SCALE * 180) / (span * 1.4 + 30)))
  return { rotate: [-lon, -lat * 0.6, 0] as [number, number, number], scale }
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
 * Coordinates come from the local airport lookup. If a code isn't in there it shows
 * a notice instead of breaking.
 */
export function RouteMap({ origin, destination }: Props) {
  const from = lookupAirport(origin)
  const to = lookupAirport(destination)

  if (!from || !to) {
    const desconocido = !from ? origin : destination
    return (
      <section className="overflow-hidden rounded-xl bg-secondary ring-1 ring-secondary ring-inset">
        <PanelHeader origin={origin} destination={destination} />
        <div className="px-5 py-5 text-sm">
          <p className="font-semibold text-primary">Mapa no disponible</p>
          <p className="mt-1 text-tertiary">
            No tengo las coordenadas del aeropuerto «{desconocido}». El mapa solo conoce una lista de aeropuertos
            principales por ahora.
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
        {from.name} → {to.name}
      </p>
      <div className="flex flex-1 items-center px-2 pb-2">
        <ComposableMap
          projection="geoEqualEarth"
          projectionConfig={frame(from.coordinates, to.coordinates)}
          width={WIDTH}
          height={HEIGHT}
          style={{ width: '100%', height: 'auto' }}
          aria-label={`Mapa de la ruta ${from.name} a ${to.name}`}
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
            <text textAnchor="middle" y={-13} fontSize={17} fontWeight={600} fontFamily={labelFont} fill={ink}>
              {origin}
            </text>
          </Marker>

          {/* Destination marker (signal). */}
          <Marker coordinates={to.coordinates}>
            <rect x={-5} y={-5} width={10} height={10} fill={signal} />
            <text textAnchor="middle" y={-13} fontSize={17} fontWeight={600} fontFamily={labelFont} fill={signal}>
              {destination}
            </text>
          </Marker>
        </ComposableMap>
      </div>
    </section>
  )
}
