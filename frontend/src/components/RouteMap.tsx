import { Alert, Paper, Title } from '@mantine/core'
import { IconMapPin } from '@tabler/icons-react'
import { ComposableMap, Geographies, Geography, Line, Marker } from 'react-simple-maps'
import { lookupAirport } from '../api/airports'

interface Props {
  origin: string
  destination: string
}

// TopoJSON del mundo servido por un CDN (patrón habitual de react-simple-maps).
const GEO_URL = 'https://cdn.jsdelivr.net/npm/world-atlas@2/countries-110m.json'

/**
 * Mapa del mundo que dibuja la ruta origen → destino de la búsqueda actual.
 *
 * Resuelve las coordenadas de cada código IATA con el diccionario local de
 * aeropuertos. Si alguno no está en el diccionario, avisa en lugar de romperse.
 */
export function RouteMap({ origin, destination }: Props) {
  const from = lookupAirport(origin)
  const to = lookupAirport(destination)

  if (!from || !to) {
    const desconocido = !from ? origin : destination
    return (
      <Paper shadow="sm" p="lg" radius="md" withBorder>
        <Alert color="yellow" icon={<IconMapPin />} title="Mapa no disponible">
          No tengo las coordenadas del aeropuerto «{desconocido}». El mapa solo conoce
          una lista de aeropuertos principales por ahora.
        </Alert>
      </Paper>
    )
  }

  return (
    <Paper shadow="sm" p="lg" radius="md" withBorder>
      <Title order={4} mb="md">
        Ruta: {from.name} ({origin}) → {to.name} ({destination})
      </Title>

      <ComposableMap
        projection="geoEqualEarth"
        projectionConfig={{ scale: 150 }}
        height={380}
        style={{ width: '100%', height: 'auto' }}
      >
        <Geographies geography={GEO_URL}>
          {({ geographies }) =>
            geographies.map((geo) => (
              <Geography
                key={geo.rsmKey}
                geography={geo}
                fill="#e9ecef"
                stroke="#ffffff"
                strokeWidth={0.5}
                style={{
                  default: { outline: 'none' },
                  hover: { fill: '#dee2e6', outline: 'none' },
                  pressed: { outline: 'none' },
                }}
              />
            ))
          }
        </Geographies>

        {/* Línea de la ruta entre los dos aeropuertos. */}
        <Line
          from={from.coordinates}
          to={to.coordinates}
          stroke="#fa5252"
          strokeWidth={2}
          strokeLinecap="round"
        />

        {/* Marcador de origen (azul). */}
        <Marker coordinates={from.coordinates}>
          <circle r={5} fill="#228be6" stroke="#fff" strokeWidth={1.5} />
          <text textAnchor="middle" y={-10} fontSize={11} fontWeight={700} fill="#228be6">
            {origin}
          </text>
        </Marker>

        {/* Marcador de destino (rojo). */}
        <Marker coordinates={to.coordinates}>
          <circle r={5} fill="#fa5252" stroke="#fff" strokeWidth={1.5} />
          <text textAnchor="middle" y={-10} fontSize={11} fontWeight={700} fill="#fa5252">
            {destination}
          </text>
        </Marker>
      </ComposableMap>
    </Paper>
  )
}
