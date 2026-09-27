import { useState } from 'react'
import { Mail01 } from '@untitledui/icons'
import { Link, useLocation } from 'react-router'
import { Button } from '@/components/base/buttons/button'
import { Input } from '@/components/base/input/input'
import { requestPasswordReset } from '../api/authApi'
import { AuthShell } from '../components/board/AuthShell'

/**
 * Asks for a reset link. The answer is the same whether the email has an account or
 * not (the backend never says), so the confirmation is worded as "if".
 */
export function ForgotPassword() {
  const location = useLocation()
  const [email, setEmail] = useState((location.state as { email?: string } | null)?.email ?? '')
  const [sentTo, setSentTo] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await requestPasswordReset(email)
      setSentTo(email)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo enviar el enlace')
    } finally {
      setSubmitting(false)
    }
  }

  const footer = (
    <>
      ¿Te has acordado?{' '}
      <Link to="/login" className="font-semibold text-brand-secondary hover:underline">
        Inicia sesión
      </Link>
    </>
  )

  if (sentTo) {
    return (
      <AuthShell title="Revisa tu correo" subtitle="Te hemos mandado un enlace para elegir otra contraseña." footer={footer}>
        <div className="flex flex-col gap-3 text-sm text-secondary" role="status">
          <p>
            Si hay una cuenta con <span className="font-semibold text-primary">{sentTo}</span>, recibirás el enlace en
            unos segundos. Caduca en 30 minutos y solo sirve una vez.
          </p>
          <p className="text-tertiary">¿No llega? Mira en la carpeta de spam o vuelve a pedirlo dentro de un minuto.</p>
          <Button color="secondary" size="md" onClick={() => setSentTo(null)}>
            Usar otro email
          </Button>
        </div>
      </AuthShell>
    )
  }

  return (
    <AuthShell
      title="Recupera tu cuenta"
      subtitle="Escribe tu email y te mandaremos un enlace para elegir una contraseña nueva."
      footer={footer}
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

        {error && (
          <p role="alert" className="rounded-lg bg-error-primary px-3 py-2 text-sm text-error-primary">
            {error}
          </p>
        )}

        <Button type="submit" size="lg" color="primary" isLoading={submitting}>
          Enviar enlace
        </Button>
      </form>
    </AuthShell>
  )
}
