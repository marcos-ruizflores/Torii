import { http } from './http'

// Histórico de precios de una ruta (mejor precio observado por día), servido por
// el backend desde la base de datos (GET /api/price-history). Se alimenta solo:
// cada búsqueda real registra su mejor precio del día. Las primeras veces la
// serie estará casi vacía — el gráfico lo indica y anima a buscar.

export interface PricePoint {
  /** Día en formato AAAA-MM-DD. */
  date: string
  /** Mejor precio observado ese día. */
  price: number
  currency: string
}

/** Serie de los últimos `days` días para una ruta. */
export async function fetchPriceHistory(
  origin: string,
  destination: string,
  days: number,
): Promise<PricePoint[]> {
  const { data } = await http.get<PricePoint[]>('/api/price-history', {
    params: { origin, destination, days },
  })
  return data
}
