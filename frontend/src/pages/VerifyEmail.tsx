import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { verifyEmail } from '../api/authApi'
import { AuthShell } from '../components/board/AuthShell'

type Status = 'checking' | 'done' | 'error'

/** Landing page of the verification link (?token=...). Works logged in or not. */
export function VerifyEmail() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const { user, updateUser } = useAuth()
  const [status, setStatus] = useState<Status>(token ? 'checking' : 'error')
  const [error, setError] = useState<string | null>(token ? null : 'Al enlace le falta el código.')
  // Tokens are single use and StrictMode runs effects twice in dev: send it once.
  const sent = useRef(false)

  useEffect(() => {
    if (!token || sent.current) return
    sent.current = true
    verifyEmail(token)
      .then((verified) => {
        setStatus('done')
        // Refresh the session profile only if it's the same account.
        if (user && user.id === verified.id) updateUser(verified)
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : 'No se pudo confirmar el email')
        setStatus('error')
      })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  const home = (
    <Link to="/" className="font-semibold text-brand-secondary hover:underline">
      Ir a Torii
    </Link>
  )

  if (status === 'checking') {
    return (
      <AuthShell title="Confirmando tu email" subtitle="Un segundo." footer={home}>
        <p className="text-sm text-secondary" role="status">
          Comprobando el enlace…
        </p>
      </AuthShell>
    )
  }

  if (status === 'done') {
    return (
      <AuthShell title="Email confirmado" subtitle="Ya puedes recuperar la cuenta si algún día olvidas la contraseña." footer={home}>
        <p className="text-sm text-secondary" role="status">
          Gracias. No tienes que hacer nada más.
        </p>
      </AuthShell>
    )
  }

  return (
    <AuthShell title="No hemos podido confirmarlo" subtitle={error ?? 'El enlace no es válido.'} footer={home}>
      <p className="text-sm text-secondary">
        {user
          ? 'Puedes pedir otro enlace desde el aviso que verás en la página principal.'
          : 'Inicia sesión y pide otro enlace desde el aviso de la página principal.'}
      </p>
    </AuthShell>
  )
}
