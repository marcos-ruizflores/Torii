import { ArrowLeft } from '@untitledui/icons'
import { useNavigate } from 'react-router'
import { Button } from '@/components/base/buttons/button'
import { AppHeader } from '../components/board/AppHeader'
import { FlapText } from '../components/board/FlapText'

/** 404 as a board row that never got a gate: the code, a heading and ways back. */
export function NotFound() {
  const navigate = useNavigate()
  return (
    <div className="min-h-dvh bg-primary">
      <main className="mx-auto flex max-w-6xl flex-col gap-8 px-4 pt-6 pb-16 sm:px-6">
        <AppHeader />
        <div className="flex max-w-xl flex-col items-start gap-6 py-12">
          <FlapText text="404" tone="signal" className="text-display-md" />
          <div className="flex flex-col gap-3">
            <h1 className="font-display text-display-sm font-semibold text-primary">Página no encontrada</h1>
            <p className="text-lg text-tertiary">
              La página que buscas no existe o se ha movido. Vuelve al panel para seguir buscando vuelos.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Button color="secondary" size="lg" iconLeading={ArrowLeft} onClick={() => navigate(-1)}>
              Volver atrás
            </Button>
            <Button color="primary" size="lg" onClick={() => navigate('/')}>
              Ir al buscador
            </Button>
          </div>
        </div>
      </main>
    </div>
  )
}
