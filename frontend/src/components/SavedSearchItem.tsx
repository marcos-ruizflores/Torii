import { RefreshCw01 } from '@untitledui/icons'
import { Badge } from '@/components/base/badges/badges'
import { Button } from '@/components/base/buttons/button'
import type { SavedSearch, SearchRequest } from '../api/types'

const PRECISION_LABEL = { FAST: 'Rápida', BALANCED: 'Equilibrada', EXHAUSTIVE: 'Exhaustiva' }

/** Convierte una búsqueda guardada en la petición exacta para repetirla. */
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
  }
}

function searchedAt(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** Una búsqueda guardada en formato fila, con su botón de repetir. */
export function SavedSearchItem({ search: s, onRepeat }: { search: SavedSearch; onRepeat: () => void }) {
  return (
    <li className="flex flex-wrap items-center justify-between gap-3 py-3">
      <div className="flex flex-col gap-1">
        <div className="flex items-center gap-2">
          <span className="font-semibold text-primary">
            {s.origin} → {s.destination}
          </span>
          <Badge type="pill-color" color="gray" size="sm">
            {PRECISION_LABEL[s.precision]}
          </Badge>
          {s.maxPrice != null && (
            <Badge type="pill-color" color="blue" size="sm">
              máx {s.maxPrice}€
            </Badge>
          )}
        </div>
        <span className="text-sm text-tertiary">
          {s.rangeStart} → {s.rangeEnd} · {s.baseDuration}
          {s.variability > 0 && `–${s.baseDuration + s.variability}`} días · {s.maxStops} escala
          {s.maxStops !== 1 ? 's' : ''} máx
        </span>
        <span className="text-xs text-quaternary">Buscada el {searchedAt(s.createdAt)}</span>
      </div>
      <Button size="sm" color="secondary" iconLeading={RefreshCw01} onClick={onRepeat}>
        Repetir
      </Button>
    </li>
  )
}
