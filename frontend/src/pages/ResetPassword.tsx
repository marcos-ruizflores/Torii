import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { Button } from '@/components/base/buttons/button'
import { Input } from '@/components/base/input/input'
import { useAuth } from '../auth/AuthContext'
import { AuthShell } from '../components/board/AuthShell'

/**
 * Landing page of the reset email (?token=...). On success the backend logs the user
 * in with the new password, so we go straight to the search page.
 */
export function ResetPassword() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const token = params.get('token')
  const { resetPassword } = useAuth()
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const footer = (
    <Link to="/olvide-contrasena" className="font-semibold text-brand-secondary hover:underline">
      Pedir un enlace nuevo
    </Link>
  )

  if (!token) {
    return (
      <AuthShell
        title="Falta el enlace"
        subtitle="Abre esta página desde el botón del correo que te enviamos, o pide un enlace nuevo."
        footer={footer}
      >
        <p className="text-sm text-secondary">Los enlaces caducan a los 30 minutos y solo sirven una vez.</p>
      </AuthShell>
    )
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    if (password !== confirm) {
      setError('Las dos contraseñas no coinciden')
      return
    }
    setSubmitting(true)
    try {
      await resetPassword(token!, password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cambiar la contraseña')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthShell
      title="Elige una contraseña nueva"
      subtitle="Al guardarla se cerrará la sesión en cualquier otro dispositivo."
      footer={footer}
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-5">
        <Input
          label="Contraseña nueva"
          type="password"
          placeholder="Mínimo 8 caracteres"
          isRequired
          minLength={8}
          value={password}
          onChange={setPassword}
        />
        <Input
          label="Repite la contraseña"
          type="password"
          placeholder="La misma otra vez"
          isRequired
          minLength={8}
          value={confirm}
          onChange={setConfirm}
        />

        {error && (
          <p role="alert" className="rounded-lg bg-error-primary px-3 py-2 text-sm text-error-primary">
            {error}
          </p>
        )}

        <Button type="submit" size="lg" color="primary" isLoading={submitting}>
          Guardar contraseña
        </Button>
      </form>
    </AuthShell>
  )
}
