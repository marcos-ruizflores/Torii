import { useState } from 'react'
import { getLocalTimeZone, today } from '@internationalized/date'
import type { DateRange } from 'react-aria-components'
import { SearchLg } from '@untitledui/icons'
import { DateRangePicker } from '@/components/application/date-picker/date-range-picker'
import { Badge } from '@/components/base/badges/badges'
import { Button } from '@/components/base/buttons/button'
import { Input } from '@/components/base/input/input'
import { InputNumber } from '@/components/base/input/input-number'
import { Select } from '@/components/base/select/select'
import type { SearchPrecision, SearchRequest } from '../api/types'

interface Props {
  onSearch: (request: SearchRequest) => void
  loading: boolean
}

const MS_PER_DAY = 86_400_000

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
): number {
  if (!range?.start || !range?.end) return 0
  const step = precision === 'FAST' ? 3 : precision === 'BALANCED' ? 2 : 1
  const tz = getLocalTimeZone()
  const rangeDays = Math.round(
    (range.end.toDate(tz).getTime() - range.start.toDate(tz).getTime()) / MS_PER_DAY,
  )

  let total = 0
  for (let d = baseDuration; d <= baseDuration + variability; d++) {
    const slots = rangeDays - d // possible departure days for this trip length
    if (slots >= 0) {
      total += Math.floor(slots / step) + 1
    }
  }
  return total
}

const PRECISION_OPTIONS = [
  { id: 'FAST', label: 'Rápida (menos consultas)' },
  { id: 'BALANCED', label: 'Equilibrada' },
  { id: 'EXHAUSTIVE', label: 'Exhaustiva' },
]

export function SearchForm({ onSearch, loading }: Props) {
  // SAFE default range relative to today (~3 lookups). Fixed dates go stale and a
  // big range would burn the API quota on the very first search.
  const defaultStart = today(getLocalTimeZone()).add({ months: 2 })

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
  const [precision, setPrecision] = useState<SearchPrecision>('EXHAUSTIVE')
  // Optional budget, null means no limit.
  const [maxPrice, setMaxPrice] = useState<number | null>(null)

  const estimatedQueries = estimateQueries(range, baseDuration, variability, precision)
  // Warning thresholds tuned for small free quotas (SerpApi is ~100/month).
  const queryColor = estimatedQueries > 100 ? 'error' : estimatedQueries > 30 ? 'warning' : 'success'

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
      precision,
    }
    // Budget is optional, only sent if the user entered a number.
    if (maxPrice !== null && maxPrice > 0) {
      request.maxPrice = maxPrice
    }
    onSearch(request)
  }

  return (
    <section className="rounded-xl bg-primary p-6 shadow-xs ring-1 ring-secondary">
      <div className="flex flex-col gap-5">
        <h2 className="text-lg font-semibold text-primary">Buscar ofertas</h2>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Input
            label="Origen (IATA)"
            placeholder="BCN"
            value={origin}
            maxLength={3}
            onChange={(value) => setOrigin(value.toUpperCase())}
          />
          <Input
            label="Destino (IATA)"
            placeholder="NRT"
            value={destination}
            maxLength={3}
            onChange={(value) => setDestination(value.toUpperCase())}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <span className="text-sm font-medium text-secondary">Rango de vacaciones</span>
          <DateRangePicker value={range} onChange={setRange} />
        </div>

        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <InputNumber
            label="Días de estancia"
            minValue={1}
            maxValue={365}
            value={baseDuration}
            onChange={(v) => setBaseDuration(Number.isNaN(v) ? 1 : v)}
          />
          <InputNumber
            label="Variabilidad (+días)"
            minValue={0}
            maxValue={30}
            value={variability}
            onChange={(v) => setVariability(Number.isNaN(v) ? 0 : v)}
          />
          <InputNumber
            label="Máx. escalas"
            minValue={0}
            maxValue={3}
            value={maxStops}
            onChange={(v) => setMaxStops(Number.isNaN(v) ? 0 : v)}
          />
          <InputNumber
            label="Nº de ofertas"
            minValue={1}
            maxValue={50}
            value={topN}
            onChange={(v) => setTopN(Number.isNaN(v) ? 1 : v)}
          />
        </div>

        <div className="flex flex-wrap items-end justify-between gap-4">
          <div className="flex flex-wrap items-end gap-4">
            <Select
              label="Precisión de la búsqueda"
              className="w-64"
              items={PRECISION_OPTIONS}
              selectedKey={precision}
              onSelectionChange={(key) => setPrecision(key as SearchPrecision)}
            >
              {(item) => <Select.Item id={item.id} label={item.label} />}
            </Select>

            <InputNumber
              label="Precio máximo (€)"
              hint="Opcional"
              placeholder="Sin límite"
              minValue={0}
              className="w-40"
              value={maxPrice ?? NaN}
              onChange={(v) => setMaxPrice(Number.isNaN(v) ? null : v)}
            />
          </div>

          <div className="flex flex-col items-end gap-2">
            <div className="flex items-center gap-2">
              <span className="text-sm text-tertiary">Esta búsqueda hará</span>
              <Badge type="pill-color" color={queryColor} size="lg">
                ~{estimatedQueries} consultas
              </Badge>
            </div>
            <Button
              size="md"
              color="primary"
              iconLeading={SearchLg}
              isLoading={loading}
              isDisabled={!canSubmit}
              onClick={handleSubmit}
            >
              Buscar
            </Button>
          </div>
        </div>

        {queryColor === 'error' && (
          <p className="text-xs text-error-primary">
            ⚠️ Son muchas consultas: pueden agotar la cuota gratuita de las APIs externas.
            Reduce el rango de fechas o usa precisión «Rápida».
          </p>
        )}
      </div>
    </section>
  )
}
