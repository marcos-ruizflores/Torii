import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { TrendDown01, TrendUp01 } from '@untitledui/icons'
import {
  Area,
  AreaChart,
  CartesianGrid,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { ChartActiveDot, ChartTooltipContent } from '@/components/application/charts/charts-base'
import { Badge } from '@/components/base/badges/badges'
import { Button } from '@/components/base/buttons/button'
import { fetchPriceHistory, type PricePoint } from '../api/priceHistory'

interface Props {
  origin: string
  destination: string
}

/** "AAAA-MM-DD" → "12 sep" para el eje X. */
function shortDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' })
}

function euros(value: number): string {
  return value.toLocaleString('es-ES', { style: 'currency', currency: 'EUR' })
}

/** Estadísticas del período que alimentan la cabecera del gráfico. */
function computeStats(points: PricePoint[]) {
  const prices = points.map((p) => p.price)
  const min = Math.min(...prices)
  const max = Math.max(...prices)
  const avg = prices.reduce((a, b) => a + b, 0) / prices.length
  const current = prices[prices.length - 1]
  const minPoint = points.find((p) => p.price === min)!
  // Cuánto está el precio de HOY por encima/debajo de la media del período.
  const deltaPct = ((current - avg) / avg) * 100
  return { min, max, avg, current, minPoint, deltaPct }
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

  const stats = history.isSuccess && history.data.length > 0 ? computeStats(history.data) : null
  const belowAverage = (stats?.deltaPct ?? 0) <= 0

  return (
    <section className="rounded-xl bg-primary p-6 shadow-xs ring-1 ring-secondary">
      {/* Cabecera: título + selector de período */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex items-center gap-2">
          <h2 className="text-lg font-semibold text-primary">
            Histórico de precios {origin} → {destination}
          </h2>
          <Badge type="pill-color" color="warning" size="sm">
            Datos simulados
          </Badge>
        </div>
        <div className="flex gap-1 rounded-lg bg-secondary p-1">
          <Button size="sm" color={days === 7 ? 'primary' : 'tertiary'} onClick={() => setDays(7)}>
            7 días
          </Button>
          <Button size="sm" color={days === 30 ? 'primary' : 'tertiary'} onClick={() => setDays(30)}>
            30 días
          </Button>
        </div>
      </div>

      {/* Resumen del período: precio actual grande + tendencia + mínimo/media */}
      {stats && (
        <div className="mt-4 mb-6 flex flex-wrap items-end gap-x-8 gap-y-3">
          <div className="flex flex-col gap-1">
            <span className="text-sm text-tertiary">Mejor precio hoy</span>
            <div className="flex items-center gap-2">
              <span className="text-display-sm font-semibold text-primary">
                {euros(stats.current)}
              </span>
              <Badge
                type="pill-color"
                color={belowAverage ? 'success' : 'error'}
                size="md"
              >
                <span className="flex items-center gap-1">
                  {belowAverage ? <TrendDown01 className="size-3.5" /> : <TrendUp01 className="size-3.5" />}
                  {Math.abs(stats.deltaPct).toFixed(1)}% vs media
                </span>
              </Badge>
            </div>
          </div>
          <div className="flex flex-col gap-1">
            <span className="text-sm text-tertiary">Mínimo del período</span>
            <span className="text-lg font-semibold text-success-primary">
              {euros(stats.min)}
              <span className="ml-1.5 text-sm font-normal text-tertiary">
                ({shortDate(stats.minPoint.date)})
              </span>
            </span>
          </div>
          <div className="flex flex-col gap-1">
            <span className="text-sm text-tertiary">Media</span>
            <span className="text-lg font-semibold text-primary">{euros(stats.avg)}</span>
          </div>
        </div>
      )}

      {history.isSuccess && stats && (
        <ResponsiveContainer width="100%" height={300}>
          <AreaChart data={history.data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
            <defs>
              <linearGradient id="priceGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="var(--color-brand-600)" stopOpacity={0.3} />
                <stop offset="100%" stopColor="var(--color-brand-600)" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid vertical={false} stroke="var(--color-border-secondary)" strokeDasharray="3 3" />
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
              width={48}
              tickFormatter={(v: number) => `${Math.round(v)}€`}
              tickLine={false}
              axisLine={false}
              tick={{ fontSize: 12, fill: 'var(--color-text-tertiary)' }}
              domain={['dataMin - 40', 'dataMax + 40']}
            />
            <Tooltip
              content={<ChartTooltipContent />}
              formatter={(value) => euros(Number(value))}
              labelFormatter={(label) => shortDate(String(label))}
              cursor={{ stroke: 'var(--color-border-secondary)' }}
            />
            {/* Suelo del período: el precio a cazar. */}
            <ReferenceLine
              y={stats.min}
              stroke="var(--color-success-500)"
              strokeDasharray="4 4"
              label={{
                value: `Mínimo ${Math.round(stats.min)}€`,
                position: 'insideBottomLeft',
                fill: 'var(--color-success-600)',
                fontSize: 12,
              }}
            />
            <Area
              type="monotone"
              dataKey="price"
              name="Mejor precio"
              stroke="var(--color-brand-600)"
              strokeWidth={2}
              fill="url(#priceGradient)"
              activeDot={<ChartActiveDot />}
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
