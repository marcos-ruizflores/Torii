import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Check, Minus } from '@untitledui/icons'
import { useNavigate } from 'react-router'
import { Button } from '@/components/base/buttons/button'
import { cx } from '@/utils/cx'
import { AppHeader } from '../components/board/AppHeader'
import { FlapText } from '../components/board/FlapText'
import { changePlan } from '../api/authApi'
import { useAuth } from '../auth/AuthContext'
import type { User } from '../api/types'

/**
 * Plans / upgrade page, based on Untitled UI's pricing pages.
 *
 * Plans map to what the backend already has: search precision
 * (FAST/BALANCED/EXHAUSTIVE) and the monthly lookup quota enforced per account.
 * There's no payment step yet, so plan changes are switched off: the cards stay
 * visible but paid plans can't be picked and every account stays on FREE.
 */

/** Flip to true (together with torii.plans.self-service-changes) once payments exist. */
const PLAN_CHANGES_ENABLED = false

interface Plan {
  key: User['plan'] // the name the backend expects (FREE/PRO/BUSINESS)
  name: string
  monthly: number // EUR/month
  description: string
  cta: string
}

const PLANS: Plan[] = [
  {
    key: 'FREE',
    name: 'Gratis',
    monthly: 0,
    description: 'Para probar Torii y buscar de vez en cuando.',
    cta: 'Empezar gratis',
  },
  {
    key: 'PRO',
    name: 'Pro',
    monthly: 9.99,
    description: 'Para quien planifica sus vacaciones a conciencia.',
    cta: 'Mejorar a Pro',
  },
  {
    key: 'BUSINESS',
    name: 'Business',
    monthly: 29.99,
    description: 'Máxima potencia: ninguna oferta se escapa.',
    cta: 'Mejorar a Business',
  },
]

/**
 * The tariff board: one row per feature, one column per plan, so the difference
 * between plans reads straight across like a fares table. Values come from each
 * plan's own feature list, in the same order.
 */
const ROWS = [
  'Consultas al mes',
  'Precisión',
  'Ofertas por búsqueda',
  'Histórico de precios',
  'Historial de búsquedas',
  'Alertas de bajada de precio',
  'Soporte prioritario',
]

/** What each plan offers per row; null means "not included". */
const TABLE: Record<Plan['key'], (string | null)[]> = {
  FREE: ['30', 'Rápida', '5', '7 días', null, null, null],
  PRO: ['500', 'Rápida y Equilibrada', '20', '30 días', 'Incluido', null, null],
  BUSINESS: ['Ilimitadas', 'Todas, también Exhaustiva', 'Ilimitadas', 'Completo', 'Incluido', 'Incluido', 'Incluido'],
}

function euros(value: number): string {
  return value === 0 ? '0 €' : `${value.toFixed(2).replace('.', ',')} €`
}

export function Pricing() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user, updateUser } = useAuth()
  const [changing, setChanging] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleChoose(plan: Plan) {
    // Not logged in: picking a plan starts with creating an account.
    if (!user) {
      navigate('/signup')
      return
    }
    setError(null)
    setChanging(plan.key)
    try {
      const updated = await changePlan(plan.key)
      updateUser(updated)
      // Quota limit changed, refresh the counter in the header.
      queryClient.invalidateQueries({ queryKey: ['my-usage'] })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cambiar el plan')
    } finally {
      setChanging(null)
    }
  }

  function action(plan: Plan) {
    if (user?.plan === plan.key) {
      return (
        <Button size="md" color="secondary" isDisabled>
          Tu plan actual
        </Button>
      )
    }
    // Without payments only signing up (to FREE) is allowed.
    if (!PLAN_CHANGES_ENABLED && (user || plan.key !== 'FREE')) {
      return (
        <Button size="md" color="secondary" isDisabled>
          Próximamente
        </Button>
      )
    }
    return (
      <Button size="md" color="primary" isLoading={changing === plan.key} onClick={() => handleChoose(plan)}>
        {plan.cta}
      </Button>
    )
  }

  return (
    <div className="min-h-dvh bg-primary">
      <main className="mx-auto flex max-w-6xl flex-col gap-8 px-4 pt-6 pb-16 sm:px-6">
        <AppHeader />

        <div className="flex flex-col gap-3 pt-2">
          <h1 className="max-w-3xl font-display text-display-sm font-semibold text-balance text-primary sm:text-display-md">
            Elige cuánta potencia de búsqueda necesitas
          </h1>
          <p className="max-w-2xl text-lg text-tertiary">
            Cada búsqueda explora decenas de combinaciones de fechas. Los planes de pago dan más consultas y
            precisiones más finas para no perderte el mejor precio.
          </p>
        </div>

        <section aria-labelledby="tariffs-title" className="overflow-hidden rounded-xl bg-secondary ring-1 ring-secondary ring-inset">
          <h2 id="tariffs-title" className="sr-only">
            Tarifas
          </h2>
          {/* Phones: one block per plan, same rows, no sideways scrolling. */}
          <div className="divide-y divide-secondary md:hidden">
            {PLANS.map((plan) => (
              <div key={plan.key} className={cx('flex flex-col gap-4 px-5 py-5', user?.plan === plan.key && 'bg-primary')}>
                <div>
                  <span className="block font-display text-lg font-semibold tracking-wide text-primary uppercase">
                    {plan.name}
                  </span>
                  <span className="mt-1 flex items-baseline gap-1">
                    <FlapText text={euros(plan.monthly)} tone="signal" still className="text-2xl" />
                    <span className="text-sm text-tertiary">al mes</span>
                  </span>
                  <span className="mt-2 block text-sm text-tertiary">{plan.description}</span>
                </div>
                <dl className="flex flex-col gap-2">
                  {ROWS.map((row, r) => {
                    const value = TABLE[plan.key][r]
                    return (
                      <div key={row} className="flex items-baseline justify-between gap-4 text-sm">
                        <dt className="text-secondary">{row}</dt>
                        <dd className={cx('text-right', value === null ? 'text-quaternary' : 'font-display font-semibold text-primary')}>
                          {value ?? 'No incluido'}
                        </dd>
                      </div>
                    )
                  })}
                </dl>
                <div>{action(plan)}</div>
              </div>
            ))}
          </div>

          <div className="hidden overflow-x-auto md:block">
            <table className="w-full min-w-[40rem] border-collapse text-left">
              <thead>
                <tr className="border-b border-secondary align-top">
                  <th scope="col" className="w-1/4 px-5 py-5">
                    <span className="board-label">Tarifa</span>
                  </th>
                  {PLANS.map((plan) => (
                    <th
                      key={plan.key}
                      scope="col"
                      className={cx('px-5 py-5 font-normal', user?.plan === plan.key && 'bg-primary')}
                    >
                      <span className="block font-display text-lg font-semibold tracking-wide text-primary uppercase">
                        {plan.name}
                      </span>
                      <span className="mt-1 flex items-baseline gap-1">
                        <FlapText text={euros(plan.monthly)} tone="signal" still className="text-2xl" />
                        <span className="text-sm text-tertiary">al mes</span>
                      </span>
                      <span className="mt-2 block max-w-[16rem] text-sm text-tertiary">{plan.description}</span>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {ROWS.map((row, r) => (
                  <tr key={row} className="border-b border-secondary">
                    <th scope="row" className="px-5 py-3.5 text-sm font-medium text-secondary">
                      {row}
                    </th>
                    {PLANS.map((plan) => {
                      const value = TABLE[plan.key][r]
                      return (
                        <td key={plan.key} className={cx('px-5 py-3.5', user?.plan === plan.key && 'bg-primary')}>
                          {value === null ? (
                            <span className="inline-flex items-center gap-1.5 text-sm text-quaternary">
                              <Minus className="size-4" aria-hidden="true" />
                              No incluido
                            </span>
                          ) : value === 'Incluido' ? (
                            <span className="inline-flex items-center gap-1.5 font-display font-semibold text-primary uppercase">
                              <Check className="size-4 text-fg-brand-primary" aria-hidden="true" />
                              Incluido
                            </span>
                          ) : (
                            <span className="font-display font-semibold text-primary">{value}</span>
                          )}
                        </td>
                      )
                    })}
                  </tr>
                ))}
                <tr>
                  <td className="px-5 py-5" />
                  {PLANS.map((plan) => (
                    <td key={plan.key} className={cx('px-5 py-5', user?.plan === plan.key && 'bg-primary')}>
                      {action(plan)}
                    </td>
                  ))}
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        {error && (
          <p role="alert" className="text-sm text-error-primary">
            {error}
          </p>
        )}

        <p className="text-sm text-tertiary">
          {PLAN_CHANGES_ENABLED
            ? 'Los pagos aún no están activos: el cambio de plan es instantáneo y gratuito mientras Torii esté en desarrollo.'
            : 'Los planes de pago llegarán pronto. De momento todas las cuentas usan el plan Gratis, con 30 consultas al mes.'}
        </p>
      </main>
    </div>
  )
}
