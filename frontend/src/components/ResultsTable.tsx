import { Plane } from '@untitledui/icons'
import { Badge } from '@/components/base/badges/badges'
import { Button } from '@/components/base/buttons/button'
import type { FlightOffer } from '../api/types'

interface Props {
  offers: FlightOffer[]
  /** IATA codes of the search (offers don't repeat them). */
  origin: string
  destination: string
}

/** "2026-09-03" -> "3 sep". */
function shortDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' })
}

/** "HH:mm:ss" -> "HH:mm" (or a dash if the source has no time). */
function shortTime(time: string | null): string {
  return time ? time.slice(0, 5) : '—'
}

/** Days between outbound and return. */
function durationDays(depart: string, ret: string): number {
  const ms = new Date(ret).getTime() - new Date(depart).getTime()
  return Math.round(ms / (1000 * 60 * 60 * 24))
}

/** One leg of the trip (outbound or return): time, date and the origin -> destination line. */
function Leg({
  time,
  date,
  from,
  to,
  stops,
  stopovers,
}: {
  time: string | null
  date: string
  from: string
  to: string
  stops?: number
  stopovers?: string[]
}) {
  return (
    <div className="flex items-center gap-4">
      <div className="w-16 shrink-0">
        <div className="text-lg font-semibold text-primary">{shortTime(time)}</div>
        <div className="text-xs text-tertiary">{shortDate(date)}</div>
      </div>

      <span className="w-11 shrink-0 text-sm font-semibold text-primary">{from}</span>

      {/* Flight line: straight if direct, with dots for each stop. */}
      <div className="relative flex flex-1 items-center">
        <div className="h-px flex-1 bg-border-secondary" />
        {stops != null && stops > 0 && (
          <>
            {Array.from({ length: stops }).map((_, i) => (
              <span key={i} className="mx-0.5 size-1.5 rounded-full bg-fg-warning-primary" />
            ))}
            <div className="h-px flex-1 bg-border-secondary" />
          </>
        )}
        <Plane className="ml-1 size-4 shrink-0 text-fg-quaternary" />
      </div>

      <span className="w-11 shrink-0 text-sm font-semibold text-primary">{to}</span>

      <div className="hidden w-32 shrink-0 text-right sm:block">
        {stops != null &&
          (stops === 0 ? (
            <Badge type="pill-color" color="success" size="sm">
              Directo
            </Badge>
          ) : (
            <Badge type="pill-color" color="warning" size="sm">
              {stops} escala{stops > 1 ? 's' : ''}
              {stopovers && stopovers.length > 0 && ` · ${stopovers.join(', ')}`}
            </Badge>
          ))}
      </div>
    </div>
  )
}

/**
 * Offers found, Skyscanner style: one card per offer, both legs on the left and
 * price + booking on the right. The price is ALWAYS the round-trip total.
 */
export function ResultsTable({ offers, origin, destination }: Props) {
  if (offers.length === 0) {
    return (
      <section className="rounded-xl bg-primary p-6 shadow-xs ring-1 ring-secondary">
        <p className="text-sm text-tertiary">No se han encontrado ofertas para esos criterios.</p>
      </section>
    )
  }

  const cheapest = offers[0].price

  return (
    <section className="flex flex-col gap-3">
      <div className="flex items-end justify-between px-1">
        <h2 className="text-lg font-semibold text-primary">Mejores {offers.length} ofertas</h2>
        <span className="text-sm text-tertiary">Precios de ida y vuelta, por viajero</span>
      </div>

      {offers.map((o, i) => {
        const extra = o.price - cheapest
        return (
          <article
            key={`${o.airline}-${o.departDate}-${i}`}
            className={`flex flex-col overflow-hidden rounded-xl bg-primary shadow-xs ring-1 sm:flex-row ${
              i === 0 ? 'ring-2 ring-brand' : 'ring-secondary'
            }`}
          >
            {/* Trip legs */}
            <div className="flex flex-1 flex-col gap-4 p-5">
              <div className="flex items-center gap-2">
                <span className="text-sm font-semibold text-primary">{o.airline}</span>
                {i === 0 && (
                  <Badge type="pill-color" color="brand" size="sm">
                    La más barata
                  </Badge>
                )}
                <span className="ml-auto text-xs text-tertiary">
                  {durationDays(o.departDate, o.returnDate)} días de estancia
                </span>
              </div>

              <Leg
                time={o.departureTime}
                date={o.departDate}
                from={origin}
                to={destination}
                stops={o.stops}
                stopovers={o.stopovers}
              />
              <Leg time={o.returnDepartureTime} date={o.returnDate} from={destination} to={origin} />
            </div>

            {/* Price and booking */}
            <div className="flex items-center justify-between gap-1 border-t border-secondary bg-secondary px-5 py-4 sm:w-52 sm:flex-col sm:items-end sm:justify-center sm:border-t-0 sm:border-l">
              <div className="text-right">
                <div className="text-xl font-semibold text-primary">
                  {o.price.toFixed(2)} {o.currency === 'EUR' ? '€' : o.currency}
                </div>
                {extra > 0 ? (
                  <div className="text-xs text-tertiary">+{extra.toFixed(2)} vs la más barata</div>
                ) : (
                  <div className="text-xs text-tertiary">ida y vuelta</div>
                )}
              </div>
              <Button size="sm" color="primary" href={o.bookingUrl} target="_blank" rel="noreferrer">
                Reservar
              </Button>
            </div>
          </article>
        )
      })}
    </section>
  )
}
