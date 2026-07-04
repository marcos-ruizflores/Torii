import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CheckCircle, Zap } from '@untitledui/icons'
import { useNavigate } from 'react-router'
import { Badge } from '@/components/base/badges/badges'
import { Button } from '@/components/base/buttons/button'
import { changePlan } from '../api/authApi'
import { useAuth } from '../auth/AuthContext'
import type { User } from '../api/types'

/**
 * Página de planes/mejora de plan, al estilo de las pricing pages de Untitled UI.
 *
 * SOLO INTERFAZ por ahora. Los planes se corresponden con lo que YA existe en el
 * backend: la precisión de búsqueda (FAST/BALANCED/EXHAUSTIVE) y el nº de
 * consultas que consume cada búsqueda. Cuando exista la base de datos de
 * usuarios, cada cuenta guardará su plan y el backend limitará las consultas
 * mensuales según él.
 */

interface Plan {
  key: User['plan'] // el nombre que entiende el backend (FREE/PRO/BUSINESS)
  name: string
  monthly: number // €/mes con pago mensual
  annualMonthly: number // €/mes equivalente con pago anual
  description: string
  features: string[]
  cta: string
  featured?: boolean
}

const PLANS: Plan[] = [
  {
    key: 'FREE',
    name: 'Gratis',
    monthly: 0,
    annualMonthly: 0,
    description: 'Para probar Torii y buscar de vez en cuando.',
    features: [
      '30 consultas al mes',
      'Precisión Rápida',
      'Top 5 ofertas por búsqueda',
      'Histórico de precios de 7 días',
    ],
    cta: 'Empezar gratis',
  },
  {
    key: 'PRO',
    name: 'Pro',
    monthly: 9.99,
    annualMonthly: 7.99,
    description: 'Para quien planifica sus vacaciones a conciencia.',
    features: [
      '500 consultas al mes',
      'Precisión Rápida y Equilibrada',
      'Top 20 ofertas por búsqueda',
      'Histórico de precios de 30 días',
      'Historial de tus búsquedas',
    ],
    cta: 'Mejorar a Pro',
    featured: true,
  },
  {
    key: 'BUSINESS',
    name: 'Business',
    monthly: 29.99,
    annualMonthly: 24.99,
    description: 'Máxima potencia: ninguna oferta se escapa.',
    features: [
      'Consultas ilimitadas',
      'Precisión Exhaustiva',
      'Ofertas ilimitadas por búsqueda',
      'Histórico de precios completo',
      'Alertas de bajada de precio',
      'Soporte prioritario',
    ],
    cta: 'Mejorar a Business',
  },
]

export function Pricing() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user, updateUser } = useAuth()
  const [billing, setBilling] = useState<'monthly' | 'annual'>('monthly')
  const [changing, setChanging] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleChoose(plan: Plan) {
    // Sin sesión, elegir plan empieza por crear la cuenta.
    if (!user) {
      navigate('/signup')
      return
    }
    setError(null)
    setChanging(plan.key)
    try {
      const updated = await changePlan(plan.key)
      updateUser(updated)
      // El límite de cuota ha cambiado: refrescar el contador de la cabecera.
      queryClient.invalidateQueries({ queryKey: ['my-usage'] })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cambiar el plan')
    } finally {
      setChanging(null)
    }
  }

  return (
    <div className="min-h-dvh bg-primary">
      <main className="mx-auto flex max-w-6xl flex-col gap-12 px-4 py-16">
        <header className="flex flex-col items-center gap-4 text-center">
          <Button color="link-gray" size="sm" iconLeading={ArrowLeft} onClick={() => navigate('/')}>
            Volver al buscador
          </Button>
          <span className="text-md font-semibold text-brand-secondary">Planes</span>
          <h1 className="text-display-md font-semibold text-primary">
            Elige cuánta potencia de búsqueda necesitas
          </h1>
          <p className="max-w-2xl text-lg text-tertiary">
            Cada búsqueda explora decenas de combinaciones de fechas. Los planes de pago te dan
            más consultas y precisiones más exhaustivas para no perderte el mejor precio.
          </p>

          <div className="mt-2 flex items-center gap-2 rounded-lg bg-secondary p-1">
            <Button
              size="sm"
              color={billing === 'monthly' ? 'primary' : 'tertiary'}
              onClick={() => setBilling('monthly')}
            >
              Mensual
            </Button>
            <Button
              size="sm"
              color={billing === 'annual' ? 'primary' : 'tertiary'}
              onClick={() => setBilling('annual')}
            >
              Anual
            </Button>
            <Badge type="pill-color" color="success" size="sm">
              −20% con pago anual
            </Badge>
          </div>
        </header>

        <section className="grid grid-cols-1 gap-6 md:grid-cols-3">
          {PLANS.map((plan) => {
            const price = billing === 'monthly' ? plan.monthly : plan.annualMonthly
            return (
              <article
                key={plan.name}
                className={`relative flex flex-col gap-6 rounded-2xl bg-primary p-8 shadow-xs ring-1 ${
                  plan.featured ? 'ring-2 ring-brand' : 'ring-secondary'
                }`}
              >
                {plan.featured && (
                  <div className="absolute -top-3 right-6">
                    <Badge type="pill-color" color="brand" size="md">
                      Más popular
                    </Badge>
                  </div>
                )}

                <div className="flex flex-col gap-2">
                  <div className="flex items-center gap-2">
                    {plan.featured && <Zap className="size-5 text-brand-secondary" />}
                    <h2 className="text-lg font-semibold text-primary">{plan.name}</h2>
                  </div>
                  <div className="flex items-end gap-1">
                    <span className="text-display-md font-semibold text-primary">
                      {price === 0 ? '0€' : `${price.toFixed(2).replace('.', ',')}€`}
                    </span>
                    <span className="pb-2 text-md text-tertiary">/mes</span>
                  </div>
                  <p className="text-md text-tertiary">{plan.description}</p>
                </div>

                <ul className="flex flex-1 flex-col gap-3">
                  {plan.features.map((feature) => (
                    <li key={feature} className="flex items-start gap-3">
                      <CheckCircle className="mt-0.5 size-5 shrink-0 text-brand-secondary" />
                      <span className="text-md text-tertiary">{feature}</span>
                    </li>
                  ))}
                </ul>

                {user?.plan === plan.key ? (
                  <Button size="lg" color="secondary" isDisabled>
                    Tu plan actual
                  </Button>
                ) : (
                  <Button
                    size="lg"
                    color={plan.featured ? 'primary' : 'secondary'}
                    isLoading={changing === plan.key}
                    onClick={() => handleChoose(plan)}
                  >
                    {plan.cta}
                  </Button>
                )}
              </article>
            )
          })}
        </section>

        {error && <p className="text-center text-sm text-error-primary">{error}</p>}

        <p className="text-center text-sm text-quaternary">
          Los pagos aún no están activos: el cambio de plan es instantáneo y gratuito mientras
          Torii esté en desarrollo.
        </p>
      </main>
    </div>
  )
}
