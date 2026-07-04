import { useState } from 'react'
import { Alert, Center, Container, Group, Loader, Stack, Text, Title } from '@mantine/core'
import { IconAlertCircle, IconPlane } from '@tabler/icons-react'
import { SearchForm } from './components/SearchForm'
import { ResultsTable } from './components/ResultsTable'
import { RouteMap } from './components/RouteMap'
import { useSearch } from './hooks/useSearch'
import type { SearchRequest } from './api/types'

export default function App() {
  const search = useSearch()
  // Guardamos la ruta de la última búsqueda para poder pintarla en el mapa.
  const [route, setRoute] = useState<{ origin: string; destination: string } | null>(null)

  function handleSearch(req: SearchRequest) {
    setRoute({ origin: req.origin, destination: req.destination })
    search.mutate(req)
  }

  return (
    <Container size="lg" py="xl">
      <Stack gap="xl">
        <Group gap="xs">
          <IconPlane size={32} />
          <div>
            <Title order={1}>Torii</Title>
            <Text c="dimmed" size="sm">
              Encuentra la mejor oferta de vuelo dentro de tu rango de vacaciones
            </Text>
          </div>
        </Group>

        <SearchForm onSearch={handleSearch} loading={search.isPending} />

        {route && <RouteMap origin={route.origin} destination={route.destination} />}

        {search.isPending && (
          <Center py="xl">
            <Stack align="center" gap="xs">
              <Loader />
              <Text c="dimmed">Buscando las mejores ofertas…</Text>
            </Stack>
          </Center>
        )}

        {search.isError && (
          <Alert color="red" icon={<IconAlertCircle />} title="No se pudo completar la búsqueda">
            {search.error.message}
          </Alert>
        )}

        {search.isSuccess && <ResultsTable offers={search.data} />}
      </Stack>
    </Container>
  )
}
