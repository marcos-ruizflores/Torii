import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { Button } from '@/components/base/buttons/button'
import { AppHeader } from '../components/board/AppHeader'
import { fetchMySearches } from '../api/authApi'
import { useAuth } from '../auth/AuthContext'
import { SavedSearchItem, toRequest } from '../components/SavedSearchItem'

/**
 * Full "my searches" page: the user's whole history (up to 50). "Repeat" goes back
 * to the search page and re-runs it automatically, App reads the {repeat}
 * navigation state and runs it on mount.
 */
export function MySearches() {
  const navigate = useNavigate()
  const { user, restoring } = useAuth()

  // Logged in users only, anonymous visitors get sent to login.
  // Wait for the stored session to be checked first, or a reload bounces to login.
  useEffect(() => {
    if (!restoring && !user) navigate('/login')
  }, [user, restoring, navigate])

  const searches = useQuery({
    queryKey: ['my-searches', 'all'],
    queryFn: () => fetchMySearches(50),
    enabled: !!user,
  })

  return (
    <div className="min-h-dvh bg-primary">
      <main className="mx-auto flex max-w-6xl flex-col gap-8 px-4 pt-6 pb-16 sm:px-6">
        <AppHeader />
        <div className="flex flex-col gap-2 pt-2">
          <h1 className="font-display text-display-sm font-semibold text-primary">Mis búsquedas</h1>
          <p className="max-w-2xl text-lg text-tertiary">
            Tu historial completo. Repite cualquiera con sus parámetros exactos.
          </p>
        </div>

        <section className="rounded-xl bg-secondary px-5 ring-1 ring-secondary ring-inset sm:px-6">
          {searches.isSuccess && searches.data.length === 0 && (
            <div className="flex flex-col items-start gap-3 py-8">
              <p className="font-semibold text-primary">Aún no tienes búsquedas guardadas.</p>
              <p className="text-sm text-tertiary">Cada vez que escaneas fechas, la búsqueda queda aquí para repetirla.</p>
              <Button color="primary" size="sm" onClick={() => navigate('/')}>
                Ir al buscador
              </Button>
            </div>
          )}
          <ul className="flex flex-col divide-y divide-secondary">
            {searches.data?.map((s) => (
              <SavedSearchItem
                key={s.id}
                search={s}
                onRepeat={() => navigate('/', { state: { repeat: toRequest(s) } })}
              />
            ))}
          </ul>
        </section>
      </main>
    </div>
  )
}
