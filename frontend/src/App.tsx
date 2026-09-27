import { lazy, Suspense, useEffect, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useLocation, useNavigate } from 'react-router'
import { Button } from '@/components/base/buttons/button'
import { AppHeader } from './components/board/AppHeader'
import { SearchForm } from './components/SearchForm'
import { ResultsBoard } from './components/ResultsTable'
import { RecentSearches } from './components/RecentSearches'
import { useAuth } from './auth/AuthContext'
import { useSearch } from './hooks/useSearch'
import { fetchMyUsage } from './api/authApi'
import type { SearchRequest } from './api/types'

// The map and the chart only show up after a search, so they load on demand and
// keep react-simple-maps and Recharts out of the first bundle.
const RouteMap = lazy(() => import('./components/RouteMap').then((m) => ({ default: m.RouteMap })))
const PriceHistoryChart = lazy(() =>
  import('./components/PriceHistoryChart').then((m) => ({ default: m.PriceHistoryChart })),
)

/** Same size as the panel it stands in for, so nothing jumps when it arrives. */
function PanelFallback() {
  return <div className="h-80 rounded-xl bg-secondary ring-1 ring-secondary ring-inset" aria-hidden="true" />
}

export default function App() {
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const { user, restoring } = useAuth()
  const search = useSearch()
  // Keep the route of the last search for the map and the price history.
  const [route, setRoute] = useState<{ origin: string; destination: string } | null>(null)

  // User's monthly quota (for the counter in the header).
  const usage = useQuery({
    queryKey: ['my-usage'],
    queryFn: fetchMyUsage,
    enabled: !!user,
  })

  function handleSearch(req: SearchRequest) {
    // Searching needs an account: with no session, instead of firing a call the
    // backend would reject with a 401, send the user to sign up.
    if (!user) {
      navigate('/signup')
      return
    }
    setRoute({ origin: req.origin, destination: req.destination })
    search.mutate(req, {
      // The search we just ran should show up in the history and the counter.
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['my-searches'] })
        queryClient.invalidateQueries({ queryKey: ['my-usage'] })
      },
      // A quota 429 also refreshes the counter (shown in the error).
      onError: () => queryClient.invalidateQueries({ queryKey: ['my-usage'] }),
    })
  }

  // "Repeat" from /mis-busquedas arrives as navigation state: run it once the session
  // is known and clear the state so it doesn't run again on every reload.
  useEffect(() => {
    if (restoring) return
    const repeat = (location.state as { repeat?: SearchRequest } | null)?.repeat
    if (repeat) {
      navigate('.', { replace: true, state: null })
      handleSearch(repeat)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [restoring])

  const remaining =
    usage.isSuccess && usage.data.limit != null ? Math.max(0, usage.data.limit - usage.data.used) : null
  const boardState = search.isPending ? 'loading' : search.isError ? 'error' : search.isSuccess ? 'success' : 'idle'

  return (
    <div className="min-h-dvh bg-primary">
      <main className="mx-auto flex max-w-6xl flex-col gap-8 px-4 pt-6 pb-16 sm:px-6">
        <AppHeader />

        <div className="flex flex-col gap-3 pt-2">
          <h1 className="max-w-3xl font-display text-display-sm font-semibold text-balance text-primary sm:text-display-md">
            Tú eliges la ventana. Torii prueba cada fecha.
          </h1>
          <p className="max-w-2xl text-lg text-tertiary">
            Encuentra la mejor oferta de vuelo dentro de tu rango de vacaciones, y si el precio es bueno de
            verdad.
          </p>
        </div>

        <SearchForm onSearch={handleSearch} loading={search.isPending} plan={user?.plan} remaining={remaining} />

        {/* Without a session the form is visible (as a showcase), but searching needs
            an account, so we invite the user to sign up for free. */}
        {!user && (
          <div className="flex flex-col items-start gap-4 rounded-xl bg-secondary px-5 py-4 ring-1 ring-secondary ring-inset sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="font-semibold text-primary">Crea una cuenta gratis para escanear fechas</p>
              <p className="mt-0.5 text-sm text-tertiary">El plan Gratis incluye 30 consultas al mes, sin tarjeta.</p>
            </div>
            <div className="flex gap-2">
              <Button color="secondary" size="sm" onClick={() => navigate('/login')}>
                Iniciar sesión
              </Button>
              <Button color="primary" size="sm" onClick={() => navigate('/signup')}>
                Crear cuenta gratis
              </Button>
            </div>
          </div>
        )}

        <ResultsBoard
          state={boardState}
          offers={search.data}
          origin={route?.origin}
          destination={route?.destination}
          errorMessage={search.error?.message}
        />

        {route && (
          <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
            <Suspense fallback={<PanelFallback />}>
              <RouteMap origin={route.origin} destination={route.destination} />
            </Suspense>
            <Suspense fallback={<PanelFallback />}>
              <PriceHistoryChart origin={route.origin} destination={route.destination} />
            </Suspense>
          </div>
        )}

        {/* Logged in users only, anonymous searches have no history. */}
        {user && <RecentSearches onRepeat={handleSearch} />}
      </main>
    </div>
  )
}
