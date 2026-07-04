import { useState } from 'react'
import {
  Badge,
  Button,
  Group,
  NumberInput,
  Paper,
  SegmentedControl,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core'
import { DatePickerInput } from '@mantine/dates'
import { IconSearch } from '@tabler/icons-react'
import type { SearchPrecision, SearchRequest } from '../api/types'

interface Props {
  onSearch: (request: SearchRequest) => void
  loading: boolean
}

/** Formato AAAA-MM-DD que espera el backend (sin desfase de zona horaria). */
function toIsoDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const MS_PER_DAY = 86_400_000

/**
 * Estima cuántas consultas hará la búsqueda (un par de fechas = una posible llamada
 * a las APIs). Replica la lógica de la ventana deslizante del backend, para avisar al
 * usuario ANTES de pulsar Buscar y no quemar cuota de las APIs externas.
 */
function estimateQueries(
  rangeStart: Date | null,
  rangeEnd: Date | null,
  baseDuration: number,
  variability: number,
  precision: SearchPrecision,
): number {
  if (!rangeStart || !rangeEnd) return 0
  const step = precision === 'FAST' ? 3 : precision === 'BALANCED' ? 2 : 1
  const rangeDays = Math.round((rangeEnd.getTime() - rangeStart.getTime()) / MS_PER_DAY)

  let total = 0
  for (let d = baseDuration; d <= baseDuration + variability; d++) {
    const slots = rangeDays - d // días de salida posibles para esta duración
    if (slots >= 0) {
      total += Math.floor(slots / step) + 1
    }
  }
  return total
}

export function SearchForm({ onSearch, loading }: Props) {
  // Valores por defecto SEGUROS: un rango pequeño que solo hace ~3 consultas, para
  // no quemar la cuota de las APIs externas con la primera búsqueda.
  const [origin, setOrigin] = useState('BCN')
  const [destination, setDestination] = useState('NRT')
  const [rangeStart, setRangeStart] = useState<Date | null>(new Date('2026-09-01'))
  const [rangeEnd, setRangeEnd] = useState<Date | null>(new Date('2026-09-17'))
  const [baseDuration, setBaseDuration] = useState<number>(14)
  const [variability, setVariability] = useState<number>(0)
  const [maxStops, setMaxStops] = useState<number>(2)
  const [topN, setTopN] = useState<number>(5)
  const [precision, setPrecision] = useState<SearchPrecision>('EXHAUSTIVE')
  // Presupuesto máximo opcional: '' = sin límite.
  const [maxPrice, setMaxPrice] = useState<number | ''>('')

  const estimatedQueries = estimateQueries(rangeStart, rangeEnd, baseDuration, variability, precision)
  // Umbrales de aviso pensando en cuotas gratuitas pequeñas (SerpApi ~100/mes).
  const queryColor = estimatedQueries > 100 ? 'red' : estimatedQueries > 30 ? 'yellow' : 'green'

  const canSubmit =
    origin.length === 3 && destination.length === 3 && rangeStart !== null && rangeEnd !== null

  function handleSubmit() {
    if (!rangeStart || !rangeEnd) return
    const request: SearchRequest = {
      origin: origin.toUpperCase(),
      destination: destination.toUpperCase(),
      rangeStart: toIsoDate(rangeStart),
      rangeEnd: toIsoDate(rangeEnd),
      baseDuration,
      variability,
      maxStops,
      topN,
      precision,
    }
    // El presupuesto es opcional: solo se envía si el usuario puso un número.
    if (maxPrice !== '' && maxPrice > 0) {
      request.maxPrice = maxPrice
    }
    onSearch(request)
  }

  return (
    <Paper shadow="sm" p="lg" radius="md" withBorder>
      <Stack gap="md">
        <Title order={4}>Buscar ofertas</Title>

        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <TextInput
            label="Origen (IATA)"
            placeholder="BCN"
            value={origin}
            maxLength={3}
            onChange={(e) => setOrigin(e.currentTarget.value.toUpperCase())}
          />
          <TextInput
            label="Destino (IATA)"
            placeholder="NRT"
            value={destination}
            maxLength={3}
            onChange={(e) => setDestination(e.currentTarget.value.toUpperCase())}
          />
        </SimpleGrid>

        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <DatePickerInput
            label="Inicio del rango de vacaciones"
            value={rangeStart}
            onChange={(value) => setRangeStart(value ? new Date(value) : null)}
          />
          <DatePickerInput
            label="Fin del rango de vacaciones"
            value={rangeEnd}
            onChange={(value) => setRangeEnd(value ? new Date(value) : null)}
          />
        </SimpleGrid>

        <SimpleGrid cols={{ base: 2, sm: 4 }}>
          <NumberInput
            label="Días de estancia"
            min={1}
            max={365}
            value={baseDuration}
            onChange={(v) => setBaseDuration(Number(v) || 1)}
          />
          <NumberInput
            label="Variabilidad (+días)"
            min={0}
            max={30}
            value={variability}
            onChange={(v) => setVariability(Number(v) || 0)}
          />
          <NumberInput
            label="Máx. escalas"
            min={0}
            max={3}
            value={maxStops}
            onChange={(v) => setMaxStops(Number(v) || 0)}
          />
          <NumberInput
            label="Nº de ofertas"
            min={1}
            max={50}
            value={topN}
            onChange={(v) => setTopN(Number(v) || 1)}
          />
        </SimpleGrid>

        <Group justify="space-between" align="flex-end" wrap="wrap">
          <Group gap="lg" align="flex-end" wrap="wrap">
            <Stack gap={4}>
              <span style={{ fontSize: 14, fontWeight: 500 }}>Precisión de la búsqueda</span>
              <SegmentedControl
                value={precision}
                onChange={(v) => setPrecision(v as SearchPrecision)}
                data={[
                  { label: 'Rápida', value: 'FAST' },
                  { label: 'Equilibrada', value: 'BALANCED' },
                  { label: 'Exhaustiva', value: 'EXHAUSTIVE' },
                ]}
              />
            </Stack>

            <NumberInput
              label="Precio máximo (€)"
              description="Opcional"
              placeholder="Sin límite"
              min={0}
              w={150}
              value={maxPrice}
              onChange={(v) => setMaxPrice(v === '' ? '' : Number(v))}
            />
          </Group>

          <Stack gap={4} align="flex-end">
            <Group gap="xs">
              <Text size="sm" c="dimmed">
                Esta búsqueda hará
              </Text>
              <Badge color={queryColor} variant="light" size="lg">
                ~{estimatedQueries} consultas
              </Badge>
            </Group>
            <Button
              leftSection={<IconSearch size={18} />}
              onClick={handleSubmit}
              loading={loading}
              disabled={!canSubmit}
              size="md"
            >
              Buscar
            </Button>
          </Stack>
        </Group>

        {queryColor === 'red' && (
          <Text size="xs" c="red">
            ⚠️ Son muchas consultas: pueden agotar la cuota gratuita de las APIs externas.
            Reduce el rango de fechas o usa precisión «Rápida».
          </Text>
        )}
      </Stack>
    </Paper>
  )
}
