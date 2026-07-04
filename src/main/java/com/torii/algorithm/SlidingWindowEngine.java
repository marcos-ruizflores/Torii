package com.torii.algorithm;

import com.torii.model.FlightOffer;
import com.torii.model.SearchRequest;
import com.torii.provider.FlightProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * El corazón de Torii: el algoritmo de "ventana deslizante" sobre fechas.
 *
 * <p>Dada una ventana de vacaciones posible (ej. 1 jul – 30 sep), una duración base
 * (ej. 14 días) y una variabilidad (ej. 3 → duraciones 14, 15, 16, 17), explora
 * todas las combinaciones de fecha de salida y duración, pregunta el precio a un
 * {@link FlightProvider} y devuelve las {@code topN} más baratas.
 *
 * <p>Las consultas son trabajo de <b>entrada/salida</b> (esperar respuestas de red),
 * así que se lanzan en paralelo sobre <b>hilos virtuales</b> (Java 21+): mientras una
 * llamada espera, su hilo se "desmonta" y el hilo real atiende otra. Un
 * {@link Semaphore} limita cuántas llamadas hay en vuelo a la vez, para respetar el
 * límite de peticiones de las APIs externas. Pasamos así de un tiempo total que era
 * la <i>suma</i> de todas las llamadas a uno cercano al de un solo lote.
 */
@Component
public class SlidingWindowEngine {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowEngine.class);

    /** Un par de fechas a consultar (una unidad de trabajo). */
    private record DatePair(LocalDate depart, LocalDate returnDate) {}

    /**
     * Orden final determinista: por precio y, para desempatar (mismo precio), por
     * fecha de salida y aerolínea. Sin este desempate, la ejecución en paralelo podría
     * devolver las ofertas empatadas en distinto orden en cada búsqueda.
     */
    private static final Comparator<FlightOffer> BY_PRICE_THEN_STABLE =
            Comparator.comparing(FlightOffer::price)
                    .thenComparing(FlightOffer::departDate)
                    .thenComparing(FlightOffer::airline);

    private final FlightProvider provider;
    private final int maxConcurrency;

    public SlidingWindowEngine(FlightProvider provider,
                               @Value("${torii.search.max-concurrency:6}") int maxConcurrency) {
        this.provider = provider;
        this.maxConcurrency = maxConcurrency;
    }

    /**
     * Ejecuta la búsqueda y devuelve las mejores ofertas ordenadas por precio.
     *
     * @param request petición ya validada
     * @return lista de hasta {@code request.topN()} ofertas, de más barata a más cara
     */
    public List<FlightOffer> findBestOffers(SearchRequest request) {
        List<DatePair> pairs = buildDatePairs(request);
        List<FlightOffer> allCandidates = queryAllInParallel(request, pairs);

        long rangeDays = ChronoUnit.DAYS.between(request.rangeStart(), request.rangeEnd());
        log.info("Búsqueda {}->{} [{}]: {} días de rango, {} consultas (concurrencia {}), {} ofertas candidatas",
                request.origin(), request.destination(), request.precision(),
                rangeDays, pairs.size(), maxConcurrency, allCandidates.size());

        return allCandidates.stream()
                // Filtro de presupuesto (si lo hay). Se aplica aquí, sobre los datos
                // ya cacheados, para que la caché sirva a cualquier presupuesto.
                .filter(offer -> request.maxPrice() == null
                        || offer.price().compareTo(request.maxPrice()) <= 0)
                .sorted(BY_PRICE_THEN_STABLE)
                .limit(request.topN())
                .toList();
    }

    /**
     * Cuántas consultas (pares de fechas) hará esta búsqueda. Lo usa el historial de
     * búsquedas y, en el futuro, el contador de cuota de cada plan.
     */
    public int countQueries(SearchRequest request) {
        return buildDatePairs(request).size();
    }

    /** Genera todos los pares (ida, vuelta) a explorar (lógica pura, sin llamadas). */
    private List<DatePair> buildDatePairs(SearchRequest request) {
        // La precisión decide cada cuántos días avanzamos la fecha de salida:
        // 1 (exhaustiva) explora todos los días; 2 o 3 saltan días para hacer menos
        // consultas a costa de poder perderse el día exacto más barato.
        int step = request.precision().dayStep();
        List<DatePair> pairs = new ArrayList<>();

        for (int duration = request.minDuration(); duration <= request.maxDuration(); duration++) {
            LocalDate lastValidDeparture = request.rangeEnd().minusDays(duration);
            for (LocalDate depart = request.rangeStart();
                 !depart.isAfter(lastValidDeparture);
                 depart = depart.plusDays(step)) {
                pairs.add(new DatePair(depart, depart.plusDays(duration)));
            }
        }
        return pairs;
    }

    /**
     * Consulta todos los pares de fechas en paralelo (hilos virtuales) con la
     * concurrencia limitada por el semáforo, y junta todas las ofertas.
     */
    private List<FlightOffer> queryAllInParallel(SearchRequest request, List<DatePair> pairs) {
        Semaphore limit = new Semaphore(maxConcurrency);
        List<FlightOffer> all = new ArrayList<>();

        // try-with-resources: al cerrar, el executor espera a que terminen las tareas.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<FlightOffer>>> futures = new ArrayList<>(pairs.size());

            for (DatePair pair : pairs) {
                futures.add(executor.submit(() -> {
                    limit.acquire(); // espera si ya hay maxConcurrency llamadas en vuelo
                    try {
                        return provider.searchOffers(
                                request.origin(), request.destination(),
                                pair.depart(), pair.returnDate(), request.maxStops());
                    } finally {
                        limit.release();
                    }
                }));
            }

            for (Future<List<FlightOffer>> future : futures) {
                try {
                    all.addAll(future.get());
                } catch (ExecutionException e) {
                    // Una fecha concreta falló (p. ej. ningún proveedor disponible).
                    // No tumbamos la búsqueda entera: seguimos con el resto.
                    log.warn("Una consulta falló y se omite: {}", e.getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return all;
    }
}
