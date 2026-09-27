import { useState } from 'react'
import { Mail01 } from '@untitledui/icons'
import { Link, useNavigate } from 'react-router'
import { Button } from '@/components/base/buttons/button'
import { Input } from '@/components/base/input/input'
import { useAuth } from '../auth/AuthContext'
import { AuthShell } from '../components/board/AuthShell'

/**
 * Login against POST /api/auth/login. On success the token is stored (AuthContext)
 * and we go back to the search page logged in.
 */
export function Login() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo iniciar sesión')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthShell
      title="Bienvenido de nuevo"
      subtitle="Inicia sesión para seguir escaneando fechas."
      footer={
        <>
          ¿No tienes cuenta?{' '}
          <Link to="/signup" className="font-semibold text-brand-secondary hover:underline">
            Regístrate gratis
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-5">
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
          placeholder="Tu contraseña"
          isRequired
          value={password}
          onChange={setPassword}
        />

        {error && (
          <p role="alert" className="rounded-lg bg-error-primary px-3 py-2 text-sm text-error-primary">
            {error}
          </p>
        )}

        <Button type="submit" size="lg" color="primary" isLoading={submitting}>
          Iniciar sesión
        </Button>
      </form>
    </AuthShell>
  )
}
