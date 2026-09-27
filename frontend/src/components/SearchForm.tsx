import { useState } from 'react'
import { getLocalTimeZone, today } from '@internationalized/date'
import type { DateRange } from 'react-aria-components'
import { AlertTriangle, SearchLg } from '@untitledui/icons'
import { DateRangePicker } from '@/components/application/date-picker/date-range-picker'
import { Button } from '@/components/base/buttons/button'
import { Input } from '@/components/base/input/input'
import { InputNumber } from '@/components/base/input/input-number'
import { Select } from '@/components/base/select/select'
import { cx } from '@/utils/cx'
import type { DayOfWeek, SearchPrecision, SearchRequest, User } from '../api/types'
import { FlapText } from './board/FlapText'
import { BOARD_COLS_LG } from './board/grid'

interface Props {
  onSearch: (request: SearchRequest) => void
  loading: boolean
  /** Plan of the logged in user; decides which precisions can be picked. */
  plan?: User['plan'] | null
  /** Lookups left this month, or null when unlimited or unknown. */
  remaining?: number | null
}

const MS_PER_DAY = 86_400_000

/**
 * Days of the week in the backend's ISO order (1 = Monday ... 7 = Sunday), which is
 * what the calculations below use.
 */
const WEEKDAYS: { id: DayOfWeek; label: string }[] = [
  { id: 'MONDAY', label: 'Lunes' },
  { id: 'TUESDAY', label: 'Martes' },
  { id: 'WEDNESDAY', label: 'Miércoles' },
  { id: 'THURSDAY', label: 'Jueves' },
  { id: 'FRIDAY', label: 'Viernes' },
  { id: 'SATURDAY', label: 'Sábado' },
  { id: 'SUNDAY', label: 'Domingo' },
]

/** ISO day number (1 = Monday ... 7 = Sunday), same as java.time.DayOfWeek. */
function isoDay(day: DayOfWeek): number {
  return WEEKDAYS.findIndex((d) => d.id === day) + 1
}

/** Nights of stay the pattern implies, same rule as WeekPattern.stayDays(). */
function stayDays(departDay: DayOfWeek, returnDay: DayOfWeek): number {
  const diff = (((isoDay(returnDay) - isoDay(departDay)) % 7) + 7) % 7
  return diff === 0 ? 7 : diff
}

/**
 * Estimates how many lookups the search will make (one date pair = one possible API
 * call). Mirrors the backend sliding window logic so the user gets warned BEFORE
 * hitting search, instead of burning external API quota.
 */
function estimateQueries(
  range: DateRange | null,
  baseDuration: number,
  variability: number,
  precision: SearchPrecision,
  weekPattern: { departDay: DayOfWeek; returnDay: DayOfWeek } | null,
): number {
  if (!range?.start || !range?.end) return 0
  const tz = getLocalTimeZone()
  const rangeDays = Math.round(
    (range.end.toDate(tz).getTime() - range.start.toDate(tz).getTime()) / MS_PER_DAY,
  )

  // Getaway mode: one lookup per week, not per day. Mirrors buildWeeklyPairs().
  if (weekPattern) {
    const stay = stayDays(weekPattern.departDay, weekPattern.returnDay)
    // Days until the first departure day inside the range (Date.getDay(): 0 = Sunday).
    const startIso = ((range.start.toDate(tz).getDay() + 6) % 7) + 1
    const offset = (((isoDay(weekPattern.departDay) - startIso) % 7) + 7) % 7
    const usableDays = rangeDays - offset - stay
    return usableDays < 0 ? 0 : Math.floor(usableDays / 7) + 1
  }

  const step = precision === 'FAST' ? 3 : precision === 'BALANCED' ? 2 : 1
  let total = 0
  for (let d = baseDuration; d <= baseDuration + variability; d++) {
    const slots = rangeDays - d // possible departure days for this trip length
    if (slots >= 0) {
      total += Math.floor(slots / step) + 1
    }
  }
  return total
}

/** Precisions each plan includes, same rule as Plan.allows() in the backend. */
const PLAN_PRECISIONS: Record<string, SearchPrecision[]> = {
  FREE: ['FAST'],
  PRO: ['FAST', 'BALANCED'],
  BUSINESS: ['FAST', 'BALANCED', 'EXHAUSTIVE'],
}

const PRECISION_OPTIONS: { id: SearchPrecision; label: string; supportingText: string; plan: string }[] = [
  { id: 'FAST', label: 'Rápida', supportingText: 'cada 3 días', plan: 'Gratis' },
  { id: 'BALANCED', label: 'Equilibrada', supportingText: 'cada 2 días', plan: 'Pro' },
  { id: 'EXHAUSTIVE', label: 'Exhaustiva', supportingText: 'cada día', plan: 'Business' },
]

/** Vacaciones / Escapadas switch: two ways of describing the trip. */
function TripModeSwitch({ weekends, onChange }: { weekends: boolean; onChange: (weekends: boolean) => void }) {
  const option = (value: boolean, label: string) => (
    <button
      type="button"
      aria-pressed={weekends === value}
      onClick={() => onChange(value)}
      className={cx(
        'rounded-md px-4 py-2 font-display text-sm font-semibold tracking-wide uppercase transition-colors duration-150',
        'outline-focus-ring focus-visible:outline-2 focus-visible:outline-offset-2',
        weekends === value ? 'bg-brand-solid text-on-signal' : 'text-tertiary hover:text-primary',
      )}
    >
      {label}
    </button>
  )
  return (
    <div className="inline-flex gap-1 rounded-lg bg-primary p-1 ring-1 ring-secondary ring-inset" role="group" aria-label="Tipo de viaje">
      {option(false, 'Vacaciones')}
      {option(true, 'Escapadas')}
    </div>
  )
}

export function SearchForm({ onSearch, loading, plan, remaining }: Props) {
  // SAFE default range relative to today (~3 lookups). Fixed dates go stale and a
  // big range would burn the API quota on the very first search.
  const defaultStart = today(getLocalTimeZone()).add({ months: 2 })
  const allowed = PLAN_PRECISIONS[plan ?? 'FREE'] ?? PLAN_PRECISIONS.FREE

  const [origin, setOrigin] = useState('BCN')
  const [destination, setDestination] = useState('NRT')
  const [range, setRange] = useState<DateRange | null>({
    start: defaultStart,
    end: defaultStart.add({ days: 16 }),
  })
  const [baseDuration, setBaseDuration] = useState(14)
  const [variability, setVariability] = useState(0)
  const [maxStops, setMaxStops] = useState(2)
  const [topN, setTopN] = useState(5)
  // Start on the finest precision the plan includes (Fast on the free plan).
  const [precision, setPrecision] = useState<SearchPrecision>(allowed[allowed.length - 1])
  // Optional budget, null means no limit.
  const [maxPrice, setMaxPrice] = useState<number | null>(null)
  // Getaway mode: only trips leaving and returning on those days of the week. While
  // it's on it overrides the trip length, so those fields get disabled.
  const [weekendsOnly, setWeekendsOnly] = useState(false)
  const [departDay, setDepartDay] = useState<DayOfWeek>('FRIDAY')
  const [returnDay, setReturnDay] = useState<DayOfWeek>('SUNDAY')

  // If the plan changes under us (e.g. after logging in), keep the precision valid.
  const effectivePrecision = allowed.includes(precision) ? precision : allowed[allowed.length - 1]

  const weekPattern = weekendsOnly ? { departDay, returnDay } : null
  const estimatedQueries = estimateQueries(range, baseDuration, variability, effectivePrecision, weekPattern)
  // Warning thresholds tuned for small free quotas (SerpApi is ~100/month).
  const load = estimatedQueries > 100 ? 'high' : estimatedQueries > 30 ? 'medium' : 'low'
  const overQuota = remaining != null && estimatedQueries > remaining

  const canSubmit = origin.length === 3 && destination.length === 3 && range !== null

  function handleSubmit() {
    if (!range?.start || !range?.end) return
    const request: SearchRequest = {
      origin: origin.toUpperCase(),
      destination: destination.toUpperCase(),
      // CalendarDate.toString() already gives "YYYY-MM-DD" with no timezone issues.
      rangeStart: range.start.toString(),
      rangeEnd: range.end.toString(),
      baseDuration,
      variability,
      maxStops,
      topN,
      precision: effectivePrecision,
    }
    // Budget is optional, only sent if the user entered a number.
    if (maxPrice !== null && maxPrice > 0) {
      request.maxPrice = maxPrice
    }
    // Both days or neither (the backend rejects just one).
    if (weekPattern) {
      request.departDayOfWeek = weekPattern.departDay
      request.returnDayOfWeek = weekPattern.returnDay
    }
    onSearch(request)
  }

  const nights = stayDays(departDay, returnDay)

  return (
    <section
      aria-labelledby="search-title"
      className="board-form rounded-xl bg-secondary py-5 ring-1 ring-secondary ring-inset sm:py-6"
    >
      <div className="flex flex-wrap items-center justify-between gap-4 px-5">
        <h2 id="search-title" className="font-display text-lg font-semibold tracking-wide text-primary uppercase">
          Tu viaje
        </h2>
        <TripModeSwitch weekends={weekendsOnly} onChange={setWeekendsOnly} />
      </div>

      {/* From lg the fields sit on the results board's columns, so the two panels line up. */}
      <div className={cx('mt-5 grid grid-cols-2 gap-x-4 gap-y-5 px-5', BOARD_COLS_LG)}>
        <Input
          label="Origen"
          placeholder="BCN"
          aria-label="Aeropuerto de origen (código IATA)"
          hint="Código IATA"
          value={origin}
          maxLength={3}
          className="lg:col-span-1"
          onChange={(value) => setOrigin(value.toUpperCase())}
        />
        <Input
          label="Destino"
          placeholder="NRT"
          aria-label="Aeropuerto de destino (código IATA)"
          hint="Código IATA"
          value={destination}
          maxLength={3}
          className="lg:col-span-1"
          onChange={(value) => setDestination(value.toUpperCase())}
        />

        <div className="col-span-2 flex flex-col gap-1.5 lg:col-span-2">
          <span className="board-label">Ventana de vacaciones</span>
          <DateRangePicker
            value={range}
            onChange={setRange}
            size="md"
            aria-label="Ventana de vacaciones"
            triggerClassName="w-full justify-start font-display text-md tracking-wide"
          />
        </div>

        {weekendsOnly ? (
          <>
            <Select
              label="Salgo el"
              items={WEEKDAYS}
              selectedKey={departDay}
              className="lg:col-span-1"
              onSelectionChange={(key) => setDepartDay(key as DayOfWeek)}
            >
              {(item) => <Select.Item id={item.id} label={item.label} />}
            </Select>
            <Select
              label="Vuelvo el"
              items={WEEKDAYS}
              selectedKey={returnDay}
              hint={`${nights} ${nights === 1 ? 'noche' : 'noches'}`}
              className="lg:col-span-1"
              onSelectionChange={(key) => setReturnDay(key as DayOfWeek)}
            >
              {(item) => <Select.Item id={item.id} label={item.label} />}
            </Select>
          </>
        ) : (
          <>
            <InputNumber
              label="Noches"
              minValue={1}
              maxValue={365}
              value={baseDuration}
              className="lg:col-span-1"
              onChange={(v) => setBaseDuration(Number.isNaN(v) ? 1 : v)}
            />
            <InputNumber
              label="Flexibilidad"
              hint="Noches de más"
              minValue={0}
              maxValue={30}
              value={variability}
              className="lg:col-span-1"
              onChange={(v) => setVariability(Number.isNaN(v) ? 0 : v)}
            />
          </>
        )}

        <InputNumber
          label="Escalas"
          hint="Como máximo"
          minValue={0}
          maxValue={3}
          value={maxStops}
          className="lg:col-span-1"
          onChange={(v) => setMaxStops(Number.isNaN(v) ? 0 : v)}
        />
        <InputNumber
          label="Ofertas"
          hint="A mostrar"
          minValue={1}
          maxValue={50}
          value={topN}
          className="lg:col-span-1"
          onChange={(v) => setTopN(Number.isNaN(v) ? 1 : v)}
        />
        <Select
          label="Precisión"
          // Precision doesn't matter in getaway mode: the step is always one week.
          hint={weekendsOnly ? 'No aplica en escapadas' : plan === 'FREE' || !plan ? 'Más con Pro y Business' : undefined}
          isDisabled={weekendsOnly}
          className="col-span-2 sm:col-span-1 lg:col-span-3"
          items={PRECISION_OPTIONS}
          selectedKey={effectivePrecision}
          disabledKeys={PRECISION_OPTIONS.filter((o) => !allowed.includes(o.id)).map((o) => o.id)}
          onSelectionChange={(key) => setPrecision(key as SearchPrecision)}
        >
          {(item) => {
            const option = PRECISION_OPTIONS.find((o) => o.id === item.id)!
            return (
              <Select.Item
                id={option.id}
                label={option.label}
                supportingText={
                  allowed.includes(option.id) ? option.supportingText : `${option.supportingText} · ${option.plan}`
                }
              />
            )
          }}
        </Select>
        <InputNumber
          label="Presupuesto"
          hint="Opcional, en euros"
          placeholder="Sin límite"
          minValue={0}
          className="col-span-2 sm:col-span-1 lg:col-span-1"
          value={maxPrice ?? NaN}
          onChange={(v) => setMaxPrice(Number.isNaN(v) ? null : v)}
        />

        <div className="col-span-2 flex flex-wrap items-end justify-between gap-4 lg:col-span-4 lg:flex-nowrap">
          <div className="flex flex-col items-start gap-1.5" aria-live="polite">
            <span className="board-label">Esta búsqueda hará</span>
            <span className="flex items-center gap-2">
              <FlapText
                text={String(estimatedQueries)}
                length={3}
                align="right"
                tone={load === 'high' || overQuota ? 'dim' : 'signal'}
                className="text-2xl"
              />
              <span
                className={cx(
                  'font-display text-sm font-semibold tracking-wide uppercase',
                  load === 'high' || overQuota ? 'text-error-primary' : load === 'medium' ? 'text-warning-primary' : 'text-secondary',
                )}
              >
                {estimatedQueries === 1 ? 'consulta' : 'consultas'}
              </span>
            </span>
          </div>
          <Button
            size="lg"
            color="primary"
            iconLeading={SearchLg}
            isLoading={loading}
            isDisabled={!canSubmit}
            onClick={handleSubmit}
          >
            Escanear fechas
          </Button>
        </div>
      </div>

      {(overQuota || load === 'high') && (
        <p className="mt-4 flex items-start gap-2 px-5 text-sm text-error-primary">
          <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          {overQuota
            ? `Te quedan ${remaining} consultas este mes y esta búsqueda necesita ${estimatedQueries}. Acorta la ventana o reduce la flexibilidad.`
            : 'Son muchas consultas: pueden agotar tu cuota del mes. Acorta la ventana o reduce la flexibilidad.'}
        </p>
      )}
    </section>
  )
}
