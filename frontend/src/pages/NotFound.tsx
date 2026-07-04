import { ArrowLeft } from '@untitledui/icons'
import { useNavigate } from 'react-router'
import { Button } from '@/components/base/buttons/button'

/**
 * Página 404 al estilo de las "404 sections" de Untitled UI: etiqueta de error,
 * titular grande, texto de apoyo y acciones para volver a terreno conocido.
 */
export function NotFound() {
  const navigate = useNavigate()

  return (
    <div className="flex min-h-dvh items-center justify-center bg-primary px-4">
      <div className="flex max-w-xl flex-col items-start gap-6 py-16">
        <div className="flex flex-col gap-3">
          <span className="text-md font-semibold text-brand-secondary">Error 404</span>
          <h1 className="text-display-lg font-semibold text-primary">Página no encontrada</h1>
          <p className="text-lg text-tertiary">
            Lo sentimos, la página que buscas no existe o se ha movido. Aquí no hay vuelos que
            rastrear ✈️
          </p>
        </div>
        <div className="flex gap-3">
          <Button color="secondary" size="lg" iconLeading={ArrowLeft} onClick={() => navigate(-1)}>
            Volver atrás
          </Button>
          <Button color="primary" size="lg" onClick={() => navigate('/')}>
            Ir al inicio
          </Button>
        </div>
      </div>
    </div>
  )
}
