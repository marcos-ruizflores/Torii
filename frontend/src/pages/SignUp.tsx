import { useState } from 'react'
import { Mail01, User01 } from '@untitledui/icons'
import { Link, useNavigate } from 'react-router'
import { Button } from '@/components/base/buttons/button'
import { Input } from '@/components/base/input/input'
import { useAuth } from '../auth/AuthContext'
import { AuthShell } from '../components/board/AuthShell'

/**
 * Sign up against POST /api/auth/signup: creates the account on the free plan and
 * logs the user in straight away (the backend returns the token with the profile).
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
    <AuthShell
      title="Crea tu cuenta"
      subtitle="Es gratis y te da 30 consultas al mes para escanear fechas."
      footer={
        <>
          ¿Ya tienes cuenta?{' '}
          <Link to="/login" className="font-semibold text-brand-secondary hover:underline">
            Inicia sesión
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-5">
        <Input label="Nombre" placeholder="Tu nombre" icon={User01} isRequired value={name} onChange={setName} />
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
          <p role="alert" className="rounded-lg bg-error-primary px-3 py-2 text-sm text-error-primary">
            {error}
          </p>
        )}

        <Button type="submit" size="lg" color="primary" isLoading={submitting}>
          Crear cuenta
        </Button>
      </form>
    </AuthShell>
  )
}
