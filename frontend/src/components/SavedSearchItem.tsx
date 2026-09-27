import { RefreshCw01 } from '@untitledui/icons'
import { Badge } from '@/components/base/badges/badges'
import { Button } from '@/components/base/buttons/button'
import type { DayOfWeek, SavedSearch, SearchRequest } from '../api/types'
import { FlapText } from './board/FlapText'

const PRECISION_LABEL = { FAST: 'Rápida', BALANCED: 'Equilibrada', EXHAUSTIVE: 'Exhaustiva' }

const DAY_LABEL: Record<DayOfWeek, string> = {
  MONDAY: 'lunes',
  TUESDAY: 'martes',
  WEDNESDAY: 'miércoles',
  THURSDAY: 'jueves',
  FRIDAY: 'viernes',
  SATURDAY: 'sábado',
  SUNDAY: 'domingo',
}

/** Turns a saved search into the exact request needed to run it again. */
export function toRequest(s: SavedSearch): SearchRequest {
  return {
    origin: s.origin,
    destination: s.destination,
    rangeStart: s.rangeStart,
    rangeEnd: s.rangeEnd,
    baseDuration: s.baseDuration,
    variability: s.variability,
    maxStops: s.maxStops,
    topN: s.topN,
    precision: s.precision,
    ...(s.maxPrice != null && s.maxPrice > 0 ? { maxPrice: s.maxPrice } : {}),
    // The getaway filter days go together or not at all.
    ...(s.departDayOfWeek && s.returnDayOfWeek
      ? { departDayOfWeek: s.departDayOfWeek, returnDayOfWeek: s.returnDayOfWeek }
      : {}),
  }
}

/** "2026-11-16" -> "16 NOV". */
function boardDate(iso: string): string {
  return new Date(iso)
    .toLocaleDateString('es-ES', { day: 'numeric', month: 'short' })
    .replace('.', '')
    .toUpperCase()
}

function searchedAt(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** A saved search as a row, with its repeat button. */
export function SavedSearchItem({ search: s, onRepeat }: { search: SavedSearch; onRepeat: () => void }) {
  // With the getaway filter the length comes from the days, not baseDuration.
  const isWeekPattern = s.departDayOfWeek != null && s.returnDayOfWeek != null
  const stayLabel = isWeekPattern
    ? `${DAY_LABEL[s.departDayOfWeek!]} → ${DAY_LABEL[s.returnDayOfWeek!]}`
    : `${s.baseDuration}${s.variability > 0 ? `–${s.baseDuration + s.variability}` : ''} noches`

  return (
    <li className="flex flex-wrap items-center justify-between gap-3 py-4">
      <div className="flex flex-col gap-1">
        <div className="flex flex-wrap items-center gap-2">
          <FlapText text={`${s.origin} → ${s.destination}`} still className="text-lg" />
          {isWeekPattern ? (
            <Badge type="color" color="brand" size="sm">
              Escapada
            </Badge>
          ) : (
            <Badge type="color" color="gray" size="sm">
              {PRECISION_LABEL[s.precision]}
            </Badge>
          )}
          {s.maxPrice != null && (
            <Badge type="color" color="gray" size="sm">
              máx. {s.maxPrice} €
            </Badge>
          )}
        </div>
        <span className="text-sm text-secondary">
          {boardDate(s.rangeStart)} — {boardDate(s.rangeEnd)} · {stayLabel} ·{' '}
          {s.maxStops === 0 ? 'sin escalas' : `hasta ${s.maxStops} ${s.maxStops === 1 ? 'escala' : 'escalas'}`}
        </span>
        <span className="text-xs text-tertiary">Buscada el {searchedAt(s.createdAt)}</span>
      </div>
      <Button size="sm" color="secondary" iconLeading={RefreshCw01} onClick={onRepeat}>
        Repetir
      </Button>
    </li>
  )
}
