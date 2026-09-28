import { AlertCircle, ArrowUpRight } from '@untitledui/icons'
import { Button } from '@/components/base/buttons/button'
import { cx } from '@/utils/cx'
import type { FlightOffer, PriceInsight } from '../api/types'
import { FlapBlank, FlapText } from './board/FlapText'
import { BOARD_COLS_MD } from './board/grid'

type BoardState = 'idle' | 'loading' | 'error' | 'success'

interface Props {
  state: BoardState
  offers?: FlightOffer[]
  /** IATA codes of the search (offers don't repeat them). */
  origin?: string
  destination?: string
  errorMessage?: string
}

/**
 * Fixed columns, like a real departures board: dates, nights and prices always line
 * up from one row to the next. Below md the row stacks into a compact block.
 */
const COLUMNS = cx('md:grid md:items-center md:gap-x-4', BOARD_COLS_MD)

/** "2026-09-03" -> "3 SEP". */
function boardDate(iso: string): string {
  return new Date(iso)
    .toLocaleDateString('es-ES', { day: 'numeric', month: 'short' })
    .replace('.', '')
    .toUpperCase()
}

/** "HH:mm:ss" -> "HH:mm" (or a dash if the source has no time). */
function shortTime(time: string | null): string {
  return time ? time.slice(0, 5) : '—'
}

/** Nights between outbound and return. */
function nights(depart: string, ret: string): number {
  const ms = new Date(ret).getTime() - new Date(depart).getTime()
  return Math.round(ms / (1000 * 60 * 60 * 24))
}

function euros(value: number, currency: string): string {
  return value.toLocaleString('es-ES', {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  })
}

const VERDICTS = {
  low: { label: 'Precio bajo', dot: 'bg-fg-success-secondary', text: 'text-success-primary' },
  typical: { label: 'Precio habitual', dot: 'bg-fg-quaternary', text: 'text-secondary' },
  high: { label: 'Precio alto', dot: 'bg-fg-warning-secondary', text: 'text-warning-primary' },
} as const

/** Where the verdict comes from, in a few words: it has to fit under the range. */
function verdictBasis(insight: PriceInsight): string | null {
  if (insight.source === 'history') return `Histórico Torii · ${insight.samples} días`
  if (insight.source === 'scan') return `Entre ${insight.samples} fechas escaneadas`
  if (insight.source === 'google') return 'Según Google'
  return null
}

/**
 * Verdict for the price, always as words plus a mark, never colour alone, with the
 * usual range and what it's based on (Torii's history, the scanned dates or Google).
 */
function Verdict({ insight, currency }: { insight: PriceInsight | null; currency: string }) {
  const verdict = insight ? VERDICTS[insight.level as keyof typeof VERDICTS] : undefined
  if (!insight || !verdict) {
    return <span className="text-sm text-quaternary">Sin veredicto</span>
  }
  const range =
    insight.typicalLow != null && insight.typicalHigh != null
      ? `Lo normal: ${Math.round(insight.typicalLow)}–${euros(insight.typicalHigh, currency)}`
      : null
  return (
    <span className="flex flex-col gap-0.5">
      <span className={cx('flex items-center gap-1.5 font-display text-sm font-semibold uppercase', verdict.text)}>
        <span className={cx('size-2 rounded-[1px]', verdict.dot)} aria-hidden="true" />
        {verdict.label}
      </span>
      {range && <span className="text-xs whitespace-nowrap text-tertiary">{range}</span>}
      {verdictBasis(insight) && (
        <span className="text-xs whitespace-nowrap text-quaternary">{verdictBasis(insight)}</span>
      )}
    </span>
  )
}

function Stops({ stops, stopovers }: { stops: number; stopovers: string[] }) {
  if (stops === 0) return <span className="font-display font-semibold text-success-primary uppercase">Directo</span>
  return (
    <span className="flex flex-col">
      <span className="font-display font-semibold text-primary uppercase">
        {stops} {stops === 1 ? 'escala' : 'escalas'}
      </span>
      {stopovers.length > 0 && <span className="text-xs text-tertiary">vía {stopovers.join(', ')}</span>}
    </span>
  )
}

function ColumnHeaders() {
  return (
    <div role="row" className={cx('hidden border-b border-secondary px-5 py-2.5', COLUMNS)}>
      {['Salida', 'Vuelta', 'Noches', 'Aerolínea', 'Escalas', 'Veredicto', 'Precio', ''].map((h, i) => (
        <span key={i} role="columnheader" className={cx('board-label', h === 'Precio' && 'text-right')}>
          {h || <span className="sr-only">Reserva</span>}
        </span>
      ))}
    </div>
  )
}

/** A placeholder row of blank flaps: the board waiting, or ticking while it scans. */
function BlankRow({ ticking }: { ticking: boolean }) {
  return (
    <div role="presentation" className={cx('border-b border-secondary px-5 py-4 last:border-b-0', COLUMNS)}>
      <FlapBlank length={6} ticking={ticking} className="text-base" />
      <FlapBlank length={6} ticking={ticking} className="hidden text-base md:inline-flex" />
      <FlapBlank length={2} ticking={ticking} className="hidden text-base md:inline-flex" />
      <FlapBlank length={8} ticking={ticking} className="hidden text-base md:inline-flex" />
      <span className="hidden md:block" />
      <span className="hidden md:block" />
      <FlapBlank length={5} ticking={ticking} className="float-right text-base md:float-none md:justify-self-end" />
      <span className="hidden md:block" />
    </div>
  )
}

function OfferRow({ offer: o, cheapest, route }: { offer: FlightOffer; cheapest: boolean; route: string }) {
  const price = cheapest ? (
    <FlapText text={euros(o.price, o.currency)} tone="signal" className="text-2xl" />
  ) : (
    <span className="font-display text-xl font-semibold text-primary">{euros(o.price, o.currency)}</span>
  )
  const book = (
    <Button
      size="sm"
      color={cheapest ? 'primary' : 'secondary'}
      iconTrailing={ArrowUpRight}
      href={o.bookingUrl}
      target="_blank"
      rel="noreferrer"
      aria-label={`Reservar ${o.airline}, ${route}, ${boardDate(o.departDate)} a ${boardDate(o.returnDate)}, ${euros(o.price, o.currency)}`}
    >
      Reservar
    </Button>
  )
  const airline = (
    <span className="flex flex-col">
      <span className="font-semibold text-primary">{o.airline}</span>
      {cheapest && (
        <span className="font-display text-xs font-semibold tracking-wide text-fg-brand-primary uppercase">
          La más barata
        </span>
      )}
    </span>
  )

  return (
    <div role="row" className={cx('border-b border-secondary last:border-b-0', cheapest && 'bg-primary')}>
      {/* Board: one fixed column per field. */}
      <div className={cx('hidden px-5 py-4', COLUMNS)}>
        <span role="cell" className="flex flex-col">
          <span className="font-display text-lg font-semibold text-primary uppercase">{boardDate(o.departDate)}</span>
          <span className="text-sm text-tertiary">{shortTime(o.departureTime)}</span>
        </span>
        <span role="cell" className="flex flex-col">
          <span className="font-display text-lg font-semibold text-primary uppercase">{boardDate(o.returnDate)}</span>
          <span className="text-sm text-tertiary">{shortTime(o.returnDepartureTime)}</span>
        </span>
        <span role="cell" className="font-display text-lg font-semibold text-primary">
          {nights(o.departDate, o.returnDate)}
        </span>
        <span role="cell">{airline}</span>
        <span role="cell">
          <Stops stops={o.stops} stopovers={o.stopovers} />
        </span>
        <span role="cell">
          <Verdict insight={o.priceInsight} currency={o.currency} />
        </span>
        <span role="cell" className="text-right">
          {price}
        </span>
        <span role="cell" className="justify-self-end">
          {book}
        </span>
      </div>

      {/* Phones: the same fields on a fixed two-column grid, identical on every row. */}
      <div className="grid grid-cols-[1fr_auto] items-start gap-x-4 gap-y-3 px-5 py-4 md:hidden">
        <span role="cell" className="flex flex-col">
          <span className="font-display text-lg font-semibold text-primary uppercase">
            {boardDate(o.departDate)} → {boardDate(o.returnDate)}
          </span>
          <span className="text-sm text-tertiary">
            Ida {shortTime(o.departureTime)} · Vuelta {shortTime(o.returnDepartureTime)}
          </span>
        </span>
        <span role="cell" className="text-right font-display text-lg font-semibold text-primary">
          {nights(o.departDate, o.returnDate)}
          <span className="ml-1 text-sm font-medium text-tertiary">noches</span>
        </span>
        <span role="cell">{airline}</span>
        <span role="cell" className="text-right">
          <Stops stops={o.stops} stopovers={o.stopovers} />
        </span>
        <span role="cell" className="self-end">
          <Verdict insight={o.priceInsight} currency={o.currency} />
        </span>
        <span role="cell" className="flex flex-col items-end gap-2">
          {price}
          {book}
        </span>
      </div>
    </div>
  )
}

/**
 * The departures board: every offer Torii found for the scanned window, cheapest on
 * top. The header works like a dictionary's guide words, naming the span in view
 * (route, first and last departure, number of rows). Prices are round-trip totals.
 */
export function ResultsBoard({ state, offers = [], origin, destination, errorMessage }: Props) {
  const route = origin && destination ? `${origin} → ${destination}` : ''
  const departures = offers.map((o) => o.departDate).sort()
  const span =
    departures.length > 0
      ? departures[0] === departures[departures.length - 1]
        ? boardDate(departures[0])
        : `${boardDate(departures[0])} — ${boardDate(departures[departures.length - 1])}`
      : null

  return (
    <section aria-labelledby="board-title" className="overflow-hidden rounded-xl bg-secondary ring-1 ring-secondary ring-inset">
      <div className="flex flex-wrap items-center justify-between gap-x-6 gap-y-2 border-b border-secondary px-5 py-4">
        <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
          <h2 id="board-title" className="font-display text-lg font-semibold tracking-wide text-primary uppercase">
            Salidas
          </h2>
          {route ? <FlapText text={route} className="text-lg" /> : <FlapBlank length={9} className="text-lg" />}
        </div>
        <p className="flex flex-wrap items-baseline gap-x-2 text-sm text-tertiary">
          {state === 'success' && offers.length > 0 && (
            <span className="board-label text-secondary">
              {span} · {offers.length} {offers.length === 1 ? 'oferta' : 'ofertas'}
            </span>
          )}
          {state === 'loading' && <span className="board-label text-secondary">Escaneando fechas</span>}
          <span>Ida y vuelta por viajero</span>
        </p>
      </div>

      <div role="table" aria-label={route ? `Ofertas ${route}` : 'Ofertas'} aria-busy={state === 'loading'}>
        {state === 'success' && offers.length > 0 && (
          <>
            <ColumnHeaders />
            <div role="rowgroup">
              {offers.map((o, i) => (
                <OfferRow key={`${o.airline}-${o.departDate}-${o.returnDate}-${i}`} offer={o} cheapest={i === 0} route={route} />
              ))}
            </div>
          </>
        )}

        {(state === 'idle' || state === 'loading') && (
          <div role="rowgroup">
            <BlankRow ticking={state === 'loading'} />
            <BlankRow ticking={state === 'loading'} />
            <BlankRow ticking={state === 'loading'} />
          </div>
        )}
      </div>

      {state === 'idle' && (
        <div className="border-t border-secondary px-5 py-5">
          <p className="font-semibold text-primary">Aún no hay salidas en el panel.</p>
          <p className="mt-1 max-w-prose text-sm text-tertiary">
            Elige una ventana de fechas y cuántas noches quieres estar. Torii probará cada combinación de ida y
            vuelta y colocará aquí las más baratas. Cuando tenga con qué comparar, te dirá también si el precio es
            bajo, habitual o alto según lo que Torii ha visto en esa ruta.
          </p>
        </div>
      )}

      {state === 'loading' && (
        <p className="border-t border-secondary px-5 py-4 text-sm text-tertiary" aria-live="polite">
          Probando cada combinación de fechas. Las búsquedas grandes pueden tardar unos segundos.
        </p>
      )}

      {state === 'error' && (
        <div className="flex items-start gap-3 px-5 py-5" role="alert">
          <AlertCircle className="mt-0.5 size-5 shrink-0 text-fg-error-primary" aria-hidden="true" />
          <div>
            <p className="font-display font-semibold tracking-wide text-error-primary uppercase">
              No se pudo completar la búsqueda
            </p>
            <p className="mt-1 text-sm text-secondary">{errorMessage}</p>
          </div>
        </div>
      )}

      {state === 'success' && offers.length === 0 && (
        <div className="px-5 py-5">
          <p className="font-semibold text-primary">Sin salidas para esos criterios.</p>
          <p className="mt-1 text-sm text-tertiary">
            Prueba a ampliar la ventana, permitir más escalas o subir el presupuesto.
          </p>
        </div>
      )}
    </section>
  )
}
