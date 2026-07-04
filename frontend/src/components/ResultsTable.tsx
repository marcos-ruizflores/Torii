import { Table, TableCard } from '@/components/application/table/table'
import { Badge } from '@/components/base/badges/badges'
import { Button } from '@/components/base/buttons/button'
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
      <section className="rounded-xl bg-primary p-6 shadow-xs ring-1 ring-secondary">
        <p className="text-sm text-tertiary">No se han encontrado ofertas para esos criterios.</p>
      </section>
    )
  }

  const cheapest = offers[0].price

  return (
    <TableCard.Root>
      <TableCard.Header
        title={`Mejores ${offers.length} ofertas`}
        description="Ordenadas de más barata a más cara"
      />
      <Table aria-label="Ofertas de vuelo" size="sm">
        <Table.Header>
          <Table.Head id="pos" label="#" isRowHeader />
          <Table.Head id="price" label="Precio" />
          <Table.Head id="airline" label="Aerolínea" />
          <Table.Head id="stops" label="Escalas" />
          <Table.Head id="depart" label="Ida" />
          <Table.Head id="return" label="Vuelta" />
          <Table.Head id="duration" label="Estancia" />
          <Table.Head id="link" label="Enlace" />
        </Table.Header>
        <Table.Body>
          {offers.map((o, i) => {
            const extra = o.price - cheapest
            return (
              <Table.Row id={`${o.airline}-${o.departDate}-${i}`} key={`${o.airline}-${o.departDate}-${i}`}>
                <Table.Cell>{i + 1}</Table.Cell>
                <Table.Cell>
                  <span className="font-semibold text-primary">
                    {o.price.toFixed(2)} {o.currency}
                  </span>
                  {extra > 0 && <div className="text-xs text-tertiary">+{extra.toFixed(2)}</div>}
                </Table.Cell>
                <Table.Cell>{o.airline}</Table.Cell>
                <Table.Cell>
                  {o.stops === 0 ? (
                    <Badge type="pill-color" color="success" size="sm">
                      Directo
                    </Badge>
                  ) : (
                    <Badge type="pill-color" color="gray" size="sm">
                      {o.stops} escala{o.stops > 1 ? 's' : ''}
                      {o.stopovers.length > 0 && ` · ${o.stopovers.join(', ')}`}
                    </Badge>
                  )}
                </Table.Cell>
                <Table.Cell>
                  {o.departDate}
                  {shortTime(o.departureTime) && (
                    <div className="text-xs text-tertiary">{shortTime(o.departureTime)}</div>
                  )}
                </Table.Cell>
                <Table.Cell>
                  {o.returnDate}
                  {shortTime(o.returnDepartureTime) && (
                    <div className="text-xs text-tertiary">{shortTime(o.returnDepartureTime)}</div>
                  )}
                </Table.Cell>
                <Table.Cell>{durationDays(o.departDate, o.returnDate)} días</Table.Cell>
                <Table.Cell>
                  <Button color="link-color" size="sm" href={o.bookingUrl} target="_blank" rel="noreferrer">
                    Reservar
                  </Button>
                </Table.Cell>
              </Table.Row>
            )
          })}
        </Table.Body>
      </Table>
    </TableCard.Root>
  )
}
