// Histórico de precios de una ruta (precio mínimo observado por día).
//
// ⚠️ DE MOMENTO SON DATOS SIMULADOS. El plan es que el backend guarde en una base
// de datos el mejor precio de cada búsqueda real y exponga un endpoint tipo
// GET /api/price-history?origin=BCN&destination=NRT&days=30. Cuando exista, esta
// función pasará a llamar a ese endpoint con axios y NADA MÁS cambiará: el
// componente del gráfico ya consume esta interfaz.

export interface PricePoint {
  /** Día en formato AAAA-MM-DD. */
  date: string
  /** Mejor precio observado ese día (EUR). */
  price: number
}

/** Hash determinista de un string (mismo truco que el MockFlightProvider del backend). */
function hashCode(text: string): number {
  let h = 0
  for (let i = 0; i < text.length; i++) {
    h = (Math.imul(31, h) + text.charCodeAt(i)) | 0
  }
  return Math.abs(h)
}

/**
 * Devuelve el histórico de los últimos `days` días para una ruta. Determinista:
 * la misma ruta produce siempre la misma serie (como el mock del backend), con un
 * precio base según la ruta, estacionalidad semanal (los findes suben) y algo de
 * ruido. La firma es async para que el cambio a la API real sea transparente.
 */
export async function fetchPriceHistory(
  origin: string,
  destination: string,
  days: number,
): Promise<PricePoint[]> {
  const seed = hashCode(`${origin}-${destination}`)
  const basePrice = 250 + (seed % 700) // 250–949 € según la ruta

  const points: PricePoint[] = []
  const now = new Date()

  for (let i = days - 1; i >= 0; i--) {
    const date = new Date(now.getTime() - i * 86_400_000)
    const dayIndex = Math.floor(date.getTime() / 86_400_000)

    // Estacionalidad semanal + oscilación lenta + ruido determinista por día.
    const weekend = [5, 6].includes(date.getDay()) ? basePrice * 0.08 : 0
    const slowWave = Math.sin(dayIndex / 9 + seed) * basePrice * 0.09
    const noise = ((hashCode(`${seed}-${dayIndex}`) % 1000) / 1000 - 0.5) * basePrice * 0.12

    points.push({
      date: date.toISOString().slice(0, 10),
      price: Math.round((basePrice + weekend + slowWave + noise) * 100) / 100,
    })
  }
  return points
}
