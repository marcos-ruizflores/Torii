import { useState } from 'react'
import { AlertCircle, Plane, Zap } from '@untitledui/icons'
import { useNavigate } from 'react-router'
import { SearchForm } from './components/SearchForm'
import { ResultsTable } from './components/ResultsTable'
import { RouteMap } from './components/RouteMap'
import { PriceHistoryChart } from './components/PriceHistoryChart'
import { Button } from '@/components/base/buttons/button'
import { useSearch } from './hooks/useSearch'
import type { SearchRequest } from './api/types'

export default function App() {
  const navigate = useNavigate()
  const search = useSearch()
  // Guardamos la ruta de la última búsqueda para el mapa y el histórico de precios.
  const [route, setRoute] = useState<{ origin: string; destination: string } | null>(null)

  function handleSearch(req: SearchRequest) {
    setRoute({ origin: req.origin, destination: req.destination })
    search.mutate(req)
  }

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
          {/* Navegación de cuenta: solo interfaz hasta que exista la BD de usuarios. */}
          <nav className="flex items-center gap-2">
            <Button color="tertiary" size="sm" iconLeading={Zap} onClick={() => navigate('/planes')}>
              Planes
            </Button>
            <Button color="secondary" size="sm" onClick={() => navigate('/login')}>
              Iniciar sesión
            </Button>
            <Button color="primary" size="sm" onClick={() => navigate('/signup')}>
              Crear cuenta
            </Button>
          </nav>
        </header>

        <SearchForm onSearch={handleSearch} loading={search.isPending} />

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

        {search.isSuccess && <ResultsTable offers={search.data} />}

        {route && <PriceHistoryChart origin={route.origin} destination={route.destination} />}
      </main>
    </div>
  )
}
