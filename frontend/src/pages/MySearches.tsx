import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft } from '@untitledui/icons'
import { useNavigate } from 'react-router'
import { Button } from '@/components/base/buttons/button'
import { fetchMySearches } from '../api/authApi'
import { useAuth } from '../auth/AuthContext'
import { SavedSearchItem, toRequest } from '../components/SavedSearchItem'

/**
 * Página completa de "Mis búsquedas": todo el historial del usuario (hasta 50).
 * "Repetir" vuelve al buscador y relanza la búsqueda automáticamente — la portada
 * (App) lee el state de navegación {repeat} y la ejecuta al montar.
 */
export function MySearches() {
  const navigate = useNavigate()
  const { user } = useAuth()

  // Página solo para usuarios con sesión: los anónimos van al login.
  useEffect(() => {
    if (!user) navigate('/login')
  }, [user, navigate])

  const searches = useQuery({
    queryKey: ['my-searches', 'all'],
    queryFn: () => fetchMySearches(50),
    enabled: !!user,
  })

  return (
    <div className="min-h-dvh bg-secondary">
      <main className="mx-auto flex max-w-3xl flex-col gap-6 px-4 py-10">
        <div>
          <Button color="link-gray" size="sm" iconLeading={ArrowLeft} onClick={() => navigate('/')}>
            Volver al buscador
          </Button>
          <h1 className="mt-2 text-display-xs font-semibold text-primary">Mis búsquedas</h1>
          <p className="text-sm text-tertiary">
            Tu historial completo. Repite cualquiera con sus parámetros exactos.
          </p>
        </div>

        <section className="rounded-xl bg-primary p-6 shadow-xs ring-1 ring-secondary">
          {searches.isSuccess && searches.data.length === 0 && (
            <p className="py-6 text-center text-sm text-tertiary">
              Aún no tienes búsquedas guardadas: haz tu primera desde el buscador.
            </p>
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
