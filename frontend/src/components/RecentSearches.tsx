import { useQuery } from '@tanstack/react-query'
import { ArrowRight } from '@untitledui/icons'
import { useNavigate } from 'react-router'
import { Button } from '@/components/base/buttons/button'
import { fetchMySearches } from '../api/authApi'
import type { SearchRequest } from '../api/types'
import { SavedSearchItem, toRequest } from './SavedSearchItem'

interface Props {
  /** Repite la búsqueda guardada (mismos parámetros que el formulario). */
  onRepeat: (request: SearchRequest) => void
}

/** Cuántas búsquedas se ven en la portada; el resto viven en /mis-busquedas. */
const SHOWN_ON_HOME = 3

/**
 * "Mis últimas búsquedas" (versión portada): las 3 más recientes del usuario, con
 * un enlace a la página completa si hay más.
 */
export function RecentSearches({ onRepeat }: Props) {
  const navigate = useNavigate()
  const searches = useQuery({
    queryKey: ['my-searches'],
    queryFn: () => fetchMySearches(10),
  })

  if (!searches.isSuccess || searches.data.length === 0) {
    return null // sin historial todavía: no ocupamos sitio
  }

  const visible = searches.data.slice(0, SHOWN_ON_HOME)
  const hasMore = searches.data.length > SHOWN_ON_HOME

  return (
    <section className="rounded-xl bg-primary p-6 shadow-xs ring-1 ring-secondary">
      <div className="mb-2 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-primary">Mis últimas búsquedas</h2>
        {hasMore && (
          <Button color="link-color" size="sm" iconTrailing={ArrowRight} onClick={() => navigate('/mis-busquedas')}>
            Ver todas
          </Button>
        )}
      </div>
      <ul className="flex flex-col divide-y divide-secondary">
        {visible.map((s) => (
          <SavedSearchItem key={s.id} search={s} onRepeat={() => onRepeat(toRequest(s))} />
        ))}
      </ul>
    </section>
  )
}
