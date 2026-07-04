import { useState } from 'react'
import { Mail01, Plane, User01 } from '@untitledui/icons'
import { Link, useNavigate } from 'react-router'
import { Button } from '@/components/base/buttons/button'
import { Input } from '@/components/base/input/input'
import { useAuth } from '../auth/AuthContext'

/**
 * Registro contra POST /api/auth/signup: crea la cuenta (plan Gratis) y deja al
 * usuario ya logueado (el backend devuelve el token junto al perfil).
 */
export function SignUp() {
  const navigate = useNavigate()
  const { signup } = useAuth()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await signup(name, email, password)
      navigate('/')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo crear la cuenta')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center bg-primary px-4 py-12">
      <div className="flex w-full max-w-sm flex-col gap-8">
        <header className="flex flex-col items-center gap-4 text-center">
          <div className="flex size-12 items-center justify-center rounded-xl bg-brand-solid text-white shadow-xs">
            <Plane className="size-6" />
          </div>
          <div className="flex flex-col gap-1">
            <h1 className="text-display-xs font-semibold text-primary">Crea tu cuenta</h1>
            <p className="text-md text-tertiary">
              Empieza gratis: tus búsquedas, tu historial de precios y tus rutas favoritas
            </p>
          </div>
        </header>

        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
          <Input
            label="Nombre"
            placeholder="Tu nombre"
            icon={User01}
            isRequired
            value={name}
            onChange={setName}
          />
          <Input
            label="Email"
            type="email"
            placeholder="tu@email.com"
            icon={Mail01}
            isRequired
            value={email}
            onChange={setEmail}
          />
          <Input
            label="Contraseña"
            type="password"
            placeholder="Crea una contraseña"
            hint="Mínimo 8 caracteres."
            isRequired
            minLength={8}
            value={password}
            onChange={setPassword}
          />

          {error && (
            <p className="rounded-lg bg-error-primary px-3 py-2 text-sm text-error-primary">
              {error}
            </p>
          )}

          <Button type="submit" size="lg" color="primary" isLoading={submitting}>
            Crear cuenta
          </Button>
        </form>

        <p className="text-center text-sm text-tertiary">
          ¿Ya tienes cuenta?{' '}
          <Link to="/login" className="font-semibold text-brand-secondary hover:underline">
            Inicia sesión
          </Link>
        </p>
      </div>
    </div>
  )
}
