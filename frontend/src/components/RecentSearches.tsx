import { useQuery } from '@tanstack/react-query'
import { ArrowRight } from '@untitledui/icons'
import { useNavigate } from 'react-router'
import { Button } from '@/components/base/buttons/button'
import { fetchMySearches } from '../api/authApi'
import type { SearchRequest } from '../api/types'
import { SavedSearchItem, toRequest } from './SavedSearchItem'

interface Props {
  /** Re-runs the saved search (same params as the form). */
  onRepeat: (request: SearchRequest) => void
}

/** How many searches show on the home page, the rest live in /mis-busquedas. */
const SHOWN_ON_HOME = 3

/**
 * Recent searches (home page version): the user's 3 latest, with a link to the full
 * page if there are more.
 */
export function RecentSearches({ onRepeat }: Props) {
  const navigate = useNavigate()
  const searches = useQuery({
    queryKey: ['my-searches'],
    queryFn: () => fetchMySearches(10),
  })

  if (!searches.isSuccess || searches.data.length === 0) {
    return null // no history yet, don't take up space
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
