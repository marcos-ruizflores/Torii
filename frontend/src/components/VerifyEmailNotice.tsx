import { useState } from 'react'
import { Button } from '@/components/base/buttons/button'
import { resendVerification } from '../api/authApi'

/** Quiet strip under the header until the account's email is confirmed. */
export function VerifyEmailNotice({ email }: { email: string }) {
  const [state, setState] = useState<'idle' | 'sending' | 'sent' | 'error'>('idle')

  async function resend() {
    setState('sending')
    try {
      await resendVerification()
      setState('sent')
    } catch {
      setState('error')
    }
  }

  return (
    <div className="flex flex-col items-start gap-3 rounded-xl bg-secondary px-5 py-3 ring-1 ring-secondary ring-inset sm:flex-row sm:items-center sm:justify-between">
      <p className="text-sm text-secondary" role={state === 'idle' ? undefined : 'status'}>
        {state === 'sent' ? (
          <>
            Enlace enviado a <span className="font-semibold text-primary">{email}</span>. Si no llega, mira en spam.
          </>
        ) : state === 'error' ? (
          'No se pudo reenviar el enlace. Prueba otra vez en un minuto.'
        ) : (
          <>
            Confirma tu email con el enlace que te enviamos a{' '}
            <span className="font-semibold text-primary">{email}</span>, así podrás recuperar la cuenta si olvidas la
            contraseña.
          </>
        )}
      </p>
      {state !== 'sent' && (
        <Button color="secondary" size="sm" isLoading={state === 'sending'} onClick={resend} className="shrink-0">
          Reenviar enlace
        </Button>
      )}
    </div>
  )
}
