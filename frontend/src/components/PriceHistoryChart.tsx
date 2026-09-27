import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
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
import { Button } from '@/components/base/buttons/button'
import { cx } from '@/utils/cx'
import { FlapText } from './board/FlapText'
import { fetchPriceHistory, type PricePoint } from '../api/priceHistory'
import { token } from '@/utils/token'

interface Props {
  origin: string
  destination: string
}

/** "YYYY-MM-DD" -> "12 sep" for the X axis. */
function shortDate(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' })
}

function percent(value: number): string {
  return `${value.toLocaleString('es-ES', { maximumFractionDigits: 1 })} %`
}

function euros(value: number): string {
  return value.toLocaleString('es-ES', { style: 'currency', currency: 'EUR' })
}

/** Stats for the selected period, shown in the chart header. */
function computeStats(points: PricePoint[]) {
  const prices = points.map((p) => p.price)
  const min = Math.min(...prices)
  const max = Math.max(...prices)
  const avg = prices.reduce((a, b) => a + b, 0) / prices.length
  const current = prices[prices.length - 1]
  const minPoint = points.find((p) => p.price === min)!
  // How far TODAY's price is above/below the period average.
  const deltaPct = ((current - avg) / avg) * 100
  return { min, max, avg, current, minPoint, deltaPct }
}

/**
 * Best price for the route over the last 7/30 days, using REAL data from the
 * database. Every search records its best price of the day, so the series grows on
 * its own as Torii gets used.
 */
export function PriceHistoryChart({ origin, destination }: Props) {
  const reducedMotion =
    typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const [days, setDays] = useState<7 | 30>(30)

  const history = useQuery({
    queryKey: ['price-history', origin, destination, days],
    queryFn: () => fetchPriceHistory(origin, destination, days),
  })

  const stats = history.isSuccess && history.data.length > 0 ? computeStats(history.data) : null
  const belowAverage = (stats?.deltaPct ?? 0) <= 0

  return (
    <section className="flex flex-col overflow-hidden rounded-xl bg-secondary ring-1 ring-secondary ring-inset">
      {/* Board panel header: name, route in flaps, period selector. */}
      <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-3 border-b border-secondary px-5 py-3">
        <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
          <h2 className="font-display text-lg font-semibold tracking-wide text-primary uppercase">Histórico</h2>
          <FlapText text={`${origin} → ${destination}`} className="text-lg" />
        </div>
        <div className="flex gap-1 rounded-lg bg-primary p-1 ring-1 ring-secondary ring-inset" role="group" aria-label="Periodo">
          <Button size="sm" color={days === 7 ? 'primary' : 'tertiary'} onClick={() => setDays(7)}>
            7 días
          </Button>
          <Button size="sm" color={days === 30 ? 'primary' : 'tertiary'} onClick={() => setDays(30)}>
            30 días
          </Button>
        </div>
      </div>

      {/* Period summary as one board row: today, low, average, same cell size. */}
      {stats && (
        <dl className="grid grid-cols-3 border-b border-secondary">
          <div className="flex flex-col gap-1 px-5 py-3">
            <dt className="board-label">Hoy</dt>
            <dd className="font-display text-xl font-semibold text-primary">{euros(stats.current)}</dd>
            {/* Sign and figure stay together; on narrow cells "vs media" drops to its own line. */}
            <dd className={cx('text-xs', belowAverage ? 'text-success-primary' : 'text-warning-primary')}>
              <span className="whitespace-nowrap">
                {belowAverage ? '−' : '+'}
                {percent(Math.abs(stats.deltaPct))}
              </span>{' '}
              <span className="whitespace-nowrap">vs media</span>
            </dd>
          </div>
          <div className="flex flex-col gap-1 border-l border-secondary px-5 py-3">
            <dt className="board-label">Mínimo</dt>
            <dd className="font-display text-xl font-semibold text-success-primary">{euros(stats.min)}</dd>
            <dd className="text-xs text-tertiary">{shortDate(stats.minPoint.date)}</dd>
          </div>
          <div className="flex flex-col gap-1 border-l border-secondary px-5 py-3">
            <dt className="board-label">Media</dt>
            <dd className="font-display text-xl font-semibold text-primary">{euros(stats.avg)}</dd>
          </div>
        </dl>
      )}

      <div className="px-5 pt-4 pb-5">
      {/* Short or empty series, history builds up as people search. */}
      {history.isSuccess && history.data.length < 2 && (
        <div className="flex flex-col items-start gap-1 rounded-lg bg-primary px-4 py-8 ring-1 ring-secondary ring-inset">
          <p className="text-sm font-medium text-secondary">
            Todavía no hay histórico suficiente para esta ruta.
          </p>
          <p className="text-sm text-tertiary">
            Cada búsqueda real registra el mejor precio del día: vuelve mañana y la curva
            empezará a dibujarse.
          </p>
        </div>
      )}

      {history.isSuccess && history.data.length >= 2 && stats && (
        <ResponsiveContainer width="100%" height={300}>
          <AreaChart data={history.data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
            <defs>
              <linearGradient id="priceGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={token('--color-brand-600')} stopOpacity={0.22} />
                <stop offset="100%" stopColor={token('--color-brand-600')} stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid vertical={false} stroke={token('--color-border-secondary')} strokeDasharray="3 3" />
            <XAxis
              dataKey="date"
              tickFormatter={shortDate}
              tickLine={false}
              axisLine={false}
              tick={{ fontSize: 12, fill: token('--color-text-tertiary') }}
              interval="preserveStartEnd"
              minTickGap={28}
            />
            <YAxis
              width={48}
              tickFormatter={(v: number) => `${Math.round(v)} €`}
              tickLine={false}
              axisLine={false}
              tick={{ fontSize: 12, fill: token('--color-text-tertiary') }}
              domain={['dataMin - 40', 'dataMax + 40']}
            />
            <Tooltip
              content={<ChartTooltipContent />}
              formatter={(value) => euros(Number(value))}
              labelFormatter={(label) => shortDate(String(label))}
              cursor={{ stroke: token('--color-border-secondary') }}
            />
            {/* Period low: the price to beat. */}
            <ReferenceLine
              y={stats.min}
              stroke={token('--color-fg-success-secondary')}
              strokeDasharray="4 4"
              label={{
                value: `Mínimo ${Math.round(stats.min)} €`,
                position: 'insideBottomLeft',
                fill: token('--color-text-success-primary'),
                fontSize: 12,
              }}
            />
            <Area
              type="monotone"
              dataKey="price"
              name="Mejor precio"
              stroke={token('--color-brand-600')}
              strokeWidth={2}
              fill="url(#priceGradient)"
              isAnimationActive={!reducedMotion}
              activeDot={<ChartActiveDot />}
            />
          </AreaChart>
        </ResponsiveContainer>
      )}

      <p className="mt-3 text-xs text-tertiary">
        El mejor precio observado cada día para esta ruta, acumulado por las búsquedas reales
        de Torii.
      </p>
      </div>
    </section>
  )
}
