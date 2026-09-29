import { cx } from '@/utils/cx'
import { lookupAirport, useAirports, type AirportIndex } from '../api/airports'
import type { FlightLeg, FlightOffer } from '../api/types'

/** A layover this long reads as "you'll be at the airport for a while". */
const LONG_LAYOVER_MIN = 6 * 60

/** "2026-12-01T21:50:00" -> "21:50", read as local time at that airport. */
function clock(iso: string | null): string {
  return iso ? iso.slice(11, 16) : '—'
}

/** Whole days between two local dates, for the "+1" next to the arrival time. */
function dayShift(from: string | null, to: string | null): number {
  if (!from || !to) return 0
  const a = Date.UTC(+from.slice(0, 4), +from.slice(5, 7) - 1, +from.slice(8, 10))
  const b = Date.UTC(+to.slice(0, 4), +to.slice(5, 7) - 1, +to.slice(8, 10))
  return Math.round((b - a) / 86_400_000)
}

/** "2026-12-01" -> "mar 1 dic". */
function longDate(iso: string): string {
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d)
    .toLocaleDateString('es-ES', { weekday: 'short', day: 'numeric', month: 'short' })
    .replace(/\./g, '')
}

/** 1790 -> "29 h 50 min". */
function duration(minutes: number | null): string | null {
  if (minutes == null) return null
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h === 0 ? `${m} min` : m === 0 ? `${h} h` : `${h} h ${m} min`
}

function city(airports: AirportIndex | undefined, iata: string): string | undefined {
  return lookupAirport(airports, iata)?.city
}

/** Airport code with its city underneath, aligned to one end of the line. */
function Endpoint({ iata, time, shift, align, airports }: {
  iata: string
  time: string
  shift?: number
  align: 'left' | 'right'
  airports: AirportIndex | undefined
}) {
  return (
    <div className={cx('flex min-w-16 flex-col', align === 'right' ? 'items-end text-right' : 'items-start')}>
      <span className="font-display text-2xl font-semibold text-primary tabular-nums">
        {time}
        {shift ? <sup className="ml-0.5 text-xs font-semibold text-fg-brand-primary">+{shift}</sup> : null}
      </span>
      <span className="font-display text-sm font-semibold tracking-wide text-secondary">{iata}</span>
      {city(airports, iata) && <span className="text-xs text-tertiary">{city(airports, iata)}</span>}
    </div>
  )
}

/**
 * One direction drawn as a route line: departure on the left, arrival on the
 * right, each stop as a hollow mark with how long you wait there. Only layovers are
 * known, not each flight's length, so stops are spaced evenly.
 */
function Leg({ label, leg, from, to, airports }: {
  label: string
  leg: FlightLeg
  from: string
  to: string
  airports: AirportIndex | undefined
}) {
  const stops = leg.layovers
  const summary = [
    leg.airline,
    duration(leg.durationMinutes),
    stops.length === 0 ? 'Directo' : `${stops.length} ${stops.length === 1 ? 'escala' : 'escalas'}`,
  ].filter(Boolean)

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <span className="board-label text-secondary">
          {label}
          {leg.departure && <span className="ml-2 text-tertiary normal-case">{longDate(leg.departure)}</span>}
        </span>
        <span className="text-sm text-tertiary">{summary.join(' · ')}</span>
      </div>

      <div className="flex items-start gap-3 sm:gap-5">
        <Endpoint iata={from} time={clock(leg.departure)} align="left" airports={airports} />

        {/* The line sits at the height of the times; stop labels hang below it. */}
        <div className={cx('relative mt-3.5 flex-1', stops.length > 0 ? 'min-h-16' : 'min-h-4')}>
          <div className="absolute inset-x-0 top-0 h-0.5 -translate-y-1/2 bg-fg-brand-primary/70" aria-hidden="true" />
          <span className="absolute top-0 left-0 size-2.5 -translate-1/2 rounded-full bg-fg-brand-primary" aria-hidden="true" />
          <span className="absolute top-0 right-0 size-2.5 translate-x-1/2 -translate-y-1/2 rounded-full bg-fg-brand-primary" aria-hidden="true" />
          {stops.map((stop, i) => {
            const long = (stop.minutes ?? 0) >= LONG_LAYOVER_MIN
            return (
              <div
                key={`${stop.airport}-${i}`}
                className="absolute top-0 flex -translate-x-1/2 flex-col items-center"
                style={{ left: `${((i + 1) / (stops.length + 1)) * 100}%` }}
              >
                <span
                  className={cx(
                    'size-3 -translate-y-1/2 rounded-full border-2 bg-secondary',
                    long ? 'border-fg-warning-secondary' : 'border-fg-brand-primary',
                  )}
                  aria-hidden="true"
                />
                <span className="font-display text-sm font-semibold tracking-wide text-primary">{stop.airport}</span>
                {stop.minutes != null && (
                  <span className={cx('text-xs whitespace-nowrap', long ? 'text-warning-primary' : 'text-tertiary')}>
                    {duration(stop.minutes)}
                    {long && ' · larga'}
                  </span>
                )}
              </div>
            )
          })}
        </div>

        <Endpoint
          iata={to}
          time={clock(leg.arrival)}
          shift={dayShift(leg.departure, leg.arrival)}
          align="right"
          airports={airports}
        />
      </div>
    </div>
  )
}

/**
 * Itinerary detail of an offer, opened from its row on the board: outbound and
 * return drawn as route lines with times, duration and each layover.
 */
export function Itinerary({ offer, origin, destination }: { offer: FlightOffer; origin: string; destination: string }) {
  const airports = useAirports()

  if (!offer.outbound && !offer.inbound) {
    return (
      <p className="text-sm text-tertiary">
        Esta fuente no da el detalle del itinerario.
        {offer.stopovers.length > 0 && ` Escalas en la ida: ${offer.stopovers.join(', ')}.`}
      </p>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      {offer.outbound && <Leg label="Ida" leg={offer.outbound} from={origin} to={destination} airports={airports.data} />}
      {offer.outbound && offer.inbound && <div className="border-t border-dashed border-secondary" aria-hidden="true" />}
      {offer.inbound && <Leg label="Vuelta" leg={offer.inbound} from={destination} to={origin} airports={airports.data} />}
    </div>
  )
}
