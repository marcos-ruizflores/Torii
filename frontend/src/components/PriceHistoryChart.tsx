import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { ChartTooltipContent } from '@/components/application/charts/charts-base'
import { Badge } from '@/components/base/badges/badges'
import { Button } from '@/components/base/buttons/button'
import { fetchPriceHistory } from '../api/priceHistory'

interface Props {
  origin: string
  destination: string
}

/** "AAAA-MM-DD" → "12 sep" para el eje X. */
function shortDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' })
}

/**
 * Evolución del mejor precio de la ruta en los últimos 7/30 días.
 *
 * Los datos vienen de fetchPriceHistory, que HOY es un mock determinista; cuando el
 * backend tenga base de datos e histórico real, solo cambiará esa función.
 */
export function PriceHistoryChart({ origin, destination }: Props) {
  const [days, setDays] = useState<7 | 30>(30)

  const history = useQuery({
    queryKey: ['price-history', origin, destination, days],
    queryFn: () => fetchPriceHistory(origin, destination, days),
  })

  return (
    <section className="rounded-xl bg-primary p-6 shadow-xs ring-1 ring-secondary">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <h2 className="text-lg font-semibold text-primary">
            Histórico de precios {origin} → {destination}
          </h2>
          <Badge type="pill-color" color="warning" size="sm">
            Datos simulados
          </Badge>
        </div>
        <div className="flex gap-2">
          <Button size="sm" color={days === 7 ? 'primary' : 'secondary'} onClick={() => setDays(7)}>
            7 días
          </Button>
          <Button size="sm" color={days === 30 ? 'primary' : 'secondary'} onClick={() => setDays(30)}>
            30 días
          </Button>
        </div>
      </div>

      {history.isSuccess && (
        <ResponsiveContainer width="100%" height={280}>
          <AreaChart data={history.data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
            <defs>
              <linearGradient id="priceGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="var(--color-brand-600)" stopOpacity={0.25} />
                <stop offset="100%" stopColor="var(--color-brand-600)" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid vertical={false} stroke="var(--color-border-secondary)" />
            <XAxis
              dataKey="date"
              tickFormatter={shortDate}
              tickLine={false}
              axisLine={false}
              tick={{ fontSize: 12, fill: 'var(--color-text-tertiary)' }}
              interval="preserveStartEnd"
              minTickGap={28}
            />
            <YAxis
              width={44}
              tickFormatter={(v: number) => `${v}€`}
              tickLine={false}
              axisLine={false}
              tick={{ fontSize: 12, fill: 'var(--color-text-tertiary)' }}
              domain={['dataMin - 40', 'dataMax + 40']}
            />
            <Tooltip
              content={<ChartTooltipContent />}
              formatter={(value) => `${Number(value).toFixed(2)} €`}
              labelFormatter={(label) => shortDate(String(label))}
              cursor={{ stroke: 'var(--color-border-secondary)' }}
            />
            <Area
              type="monotone"
              dataKey="price"
              name="Mejor precio"
              stroke="var(--color-brand-600)"
              strokeWidth={2}
              fill="url(#priceGradient)"
            />
          </AreaChart>
        </ResponsiveContainer>
      )}

      <p className="mt-3 text-xs text-tertiary">
        El mejor precio observado cada día para esta ruta. Cuando Torii tenga base de datos,
        aquí se verá el histórico real acumulado por tus búsquedas.
      </p>
    </section>
  )
}
