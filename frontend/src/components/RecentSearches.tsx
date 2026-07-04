import { useQuery } from '@tanstack/react-query'
import { RefreshCw01 } from '@untitledui/icons'
import { Badge } from '@/components/base/badges/badges'
import { Button } from '@/components/base/buttons/button'
import { fetchMySearches } from '../api/authApi'
import type { SavedSearch, SearchRequest } from '../api/types'

interface Props {
  /** Repite la búsqueda guardada (mismos parámetros que el formulario). */
  onRepeat: (request: SearchRequest) => void
}

const PRECISION_LABEL = { FAST: 'Rápida', BALANCED: 'Equilibrada', EXHAUSTIVE: 'Exhaustiva' }

function toRequest(s: SavedSearch): SearchRequest {
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

function relativeDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/**
 * "Mis últimas búsquedas": las búsquedas guardadas del usuario autenticado, con un
 * botón para repetir cada una con exactamente los mismos parámetros.
 */
export function RecentSearches({ onRepeat }: Props) {
  const searches = useQuery({
    queryKey: ['my-searches'],
    queryFn: fetchMySearches,
  })

  if (!searches.isSuccess || searches.data.length === 0) {
    return null // sin historial todavía: no ocupamos sitio
  }

  return (
    <section className="rounded-xl bg-primary p-6 shadow-xs ring-1 ring-secondary">
      <h2 className="mb-4 text-lg font-semibold text-primary">Mis últimas búsquedas</h2>
      <ul className="flex flex-col divide-y divide-secondary">
        {searches.data.map((s) => (
          <li key={s.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
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
                {s.variability > 0 && `–${s.baseDuration + s.variability}`} días ·{' '}
                {s.maxStops} escala{s.maxStops !== 1 ? 's' : ''} máx
              </span>
              <span className="text-xs text-quaternary">Buscada el {relativeDate(s.createdAt)}</span>
            </div>
            <Button size="sm" color="secondary" iconLeading={RefreshCw01} onClick={() => onRepeat(toRequest(s))}>
              Repetir
            </Button>
          </li>
        ))}
      </ul>
    </section>
  )
}
