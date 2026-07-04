import { Anchor, Badge, Paper, Table, Text, Title } from '@mantine/core'
import type { FlightOffer } from '../api/types'

interface Props {
  offers: FlightOffer[]
}

/** Días de estancia entre ida y vuelta. */
function durationDays(depart: string, ret: string): number {
  const ms = new Date(ret).getTime() - new Date(depart).getTime()
  return Math.round(ms / (1000 * 60 * 60 * 24))
}

/** "HH:mm:ss" → "HH:mm" (o cadena vacía si no hay hora). */
function shortTime(time: string | null): string {
  return time ? time.slice(0, 5) : ''
}

export function ResultsTable({ offers }: Props) {
  if (offers.length === 0) {
    return (
      <Paper p="lg" radius="md" withBorder>
        <Text c="dimmed">No se han encontrado ofertas para esos criterios.</Text>
      </Paper>
    )
  }

  const cheapest = offers[0].price

  const rows = offers.map((o, i) => {
    const extra = o.price - cheapest
    return (
      <Table.Tr key={`${o.airline}-${o.departDate}-${i}`}>
        <Table.Td>{i + 1}</Table.Td>
        <Table.Td>
          <Text fw={700}>
            {o.price.toFixed(2)} {o.currency}
          </Text>
          {extra > 0 && (
            <Text size="xs" c="dimmed">
              +{extra.toFixed(2)}
            </Text>
          )}
        </Table.Td>
        <Table.Td>{o.airline}</Table.Td>
        <Table.Td>
          {o.stops === 0 ? (
            <Badge color="green" variant="light">
              Directo
            </Badge>
          ) : (
            <Badge color="gray" variant="light">
              {o.stops} escala{o.stops > 1 ? 's' : ''}
              {o.stopovers.length > 0 && ` · ${o.stopovers.join(', ')}`}
            </Badge>
          )}
        </Table.Td>
        <Table.Td>
          {o.departDate}
          {shortTime(o.departureTime) && (
            <Text size="xs" c="dimmed">
              {shortTime(o.departureTime)}
            </Text>
          )}
        </Table.Td>
        <Table.Td>
          {o.returnDate}
          {shortTime(o.returnDepartureTime) && (
            <Text size="xs" c="dimmed">
              {shortTime(o.returnDepartureTime)}
            </Text>
          )}
        </Table.Td>
        <Table.Td>{durationDays(o.departDate, o.returnDate)} días</Table.Td>
        <Table.Td>
          <Anchor href={o.bookingUrl} target="_blank" rel="noreferrer">
            Reservar
          </Anchor>
        </Table.Td>
      </Table.Tr>
    )
  })

  return (
    <Paper shadow="sm" p="lg" radius="md" withBorder>
      <Title order={4} mb="md">
        Mejores {offers.length} ofertas
      </Title>
      <Table.ScrollContainer minWidth={700}>
        <Table striped highlightOnHover verticalSpacing="sm">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>#</Table.Th>
              <Table.Th>Precio</Table.Th>
              <Table.Th>Aerolínea</Table.Th>
              <Table.Th>Escalas</Table.Th>
              <Table.Th>Ida</Table.Th>
              <Table.Th>Vuelta</Table.Th>
              <Table.Th>Estancia</Table.Th>
              <Table.Th>Enlace</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>{rows}</Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Paper>
  )
}
