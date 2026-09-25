import { useEffect, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertCircle, LogOut01, Plane, Zap } from '@untitledui/icons'
import { useLocation, useNavigate } from 'react-router'
import { SearchForm } from './components/SearchForm'
import { ResultsTable } from './components/ResultsTable'
import { RouteMap } from './components/RouteMap'
import { PriceHistoryChart } from './components/PriceHistoryChart'
import { Badge } from '@/components/base/badges/badges'
import { Button } from '@/components/base/buttons/button'
import { RecentSearches } from './components/RecentSearches'
import { useAuth } from './auth/AuthContext'
import { useSearch } from './hooks/useSearch'
import { fetchMyUsage } from './api/authApi'
import type { SearchRequest } from './api/types'

export default function App() {
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const { user, logout } = useAuth()
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

  // "Repeat" from /mis-busquedas arrives as navigation state: run it on mount and
  // clear the state so it doesn't run again on every reload.
  useEffect(() => {
    const repeat = (location.state as { repeat?: SearchRequest } | null)?.repeat
    if (repeat) {
      navigate('.', { replace: true, state: null })
      handleSearch(repeat)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="min-h-dvh bg-secondary">
      <main className="mx-auto flex max-w-5xl flex-col gap-8 px-4 py-10">
        <header className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex size-12 items-center justify-center rounded-lg bg-brand-solid text-white">
              <Plane className="size-6" />
            </div>
            <div>
              <h1 className="text-display-xs font-semibold text-primary">Torii</h1>
              <p className="text-sm text-tertiary">
                Encuentra la mejor oferta de vuelo dentro de tu rango de vacaciones
              </p>
            </div>
          </div>
          <nav className="flex items-center gap-2">
            <Button color="tertiary" size="sm" iconLeading={Zap} onClick={() => navigate('/planes')}>
              Planes
            </Button>
            {user ? (
              <>
                <span className="text-sm text-secondary">
                  Hola, <span className="font-semibold text-primary">{user.name}</span>
                </span>
                <Badge type="pill-color" color={user.plan === 'FREE' ? 'gray' : 'brand'} size="sm">
                  {user.plan}
                </Badge>
                {usage.isSuccess && usage.data.limit != null && (
                  <Badge
                    type="pill-color"
                    size="sm"
                    color={usage.data.used >= usage.data.limit ? 'error' : 'success'}
                  >
                    {usage.data.used}/{usage.data.limit} consultas
                  </Badge>
                )}
                <Button color="secondary" size="sm" iconLeading={LogOut01} onClick={logout}>
                  Salir
                </Button>
              </>
            ) : (
              <>
                <Button color="secondary" size="sm" onClick={() => navigate('/login')}>
                  Iniciar sesión
                </Button>
                <Button color="primary" size="sm" onClick={() => navigate('/signup')}>
                  Crear cuenta
                </Button>
              </>
            )}
          </nav>
        </header>

        <SearchForm onSearch={handleSearch} loading={search.isPending} />

        {/* Logged in users only, anonymous searches have no history. */}
        {user && <RecentSearches onRepeat={handleSearch} />}

        {route && <RouteMap origin={route.origin} destination={route.destination} />}

        {search.isPending && (
          <div className="flex flex-col items-center gap-3 py-10">
            <div className="size-8 animate-spin rounded-full border-2 border-brand-600 border-t-transparent" />
            <p className="text-sm text-tertiary">Buscando las mejores ofertas…</p>
          </div>
        )}

        {search.isError && (
          <div className="flex items-start gap-3 rounded-xl bg-error-primary p-4 ring-1 ring-error_subtle ring-inset">
            <AlertCircle className="mt-0.5 size-5 shrink-0 text-error-primary" />
            <div className="text-sm">
              <p className="font-medium text-error-primary">No se pudo completar la búsqueda</p>
              <p className="mt-0.5 text-error-primary">{search.error.message}</p>
            </div>
          </div>
        )}

        {search.isSuccess && route && (
          <ResultsTable offers={search.data} origin={route.origin} destination={route.destination} />
        )}

        {route && <PriceHistoryChart origin={route.origin} destination={route.destination} />}
      </main>
    </div>
  )
}
