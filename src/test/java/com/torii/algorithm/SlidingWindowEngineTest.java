package com.torii.algorithm;

import com.torii.model.FlightOffer;
import com.torii.model.SearchPrecision;
import com.torii.model.SearchRequest;
import com.torii.provider.FlightProvider;
import com.torii.provider.MockFlightProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

/**
 * Tests del motor de ventana deslizante usando el provider mock (determinista).
 *
 * <p>Comprueban las garantías del algoritmo, no precios concretos: que respeta
 * topN, que el resultado está ordenado por precio, que toda oferta cabe dentro del
 * rango y respeta las duraciones pedidas, y que es reproducible.
 */
class SlidingWindowEngineTest {

    private static final int CONCURRENCY = 4;
    private final SlidingWindowEngine engine =
            new SlidingWindowEngine(new MockFlightProvider(), CONCURRENCY);

    private SearchRequest sampleRequest() {
        // Rango Jul–Sep, estancia base 14 días, variabilidad 3 (→ 14..17), top 5.
        return new SearchRequest(
                "BCN", "NRT",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 30),
                14, 3, 1, 5);
    }

    @Test
    void devuelveComoMuchoTopN() {
        List<FlightOffer> result = engine.findBestOffers(sampleRequest());
        assertThat(result).hasSizeLessThanOrEqualTo(5).isNotEmpty();
    }

    @Test
    void resultadosOrdenadosPorPrecioAscendente() {
        List<FlightOffer> result = engine.findBestOffers(sampleRequest());
        assertThat(result).isSortedAccordingTo(
                (a, b) -> a.price().compareTo(b.price()));
    }

    @Test
    void todaOfertaRespetaRangoYDuraciones() {
        SearchRequest req = sampleRequest();
        List<FlightOffer> result = engine.findBestOffers(req);

        assertThat(result).allSatisfy(offer -> {
            assertThat(offer.departDate()).isAfterOrEqualTo(req.rangeStart());
            assertThat(offer.returnDate()).isBeforeOrEqualTo(req.rangeEnd());
            assertThat(offer.durationDays())
                    .isBetween((long) req.minDuration(), (long) req.maxDuration());
        });
    }

    @Test
    void esReproducible() {
        List<FlightOffer> first = engine.findBestOffers(sampleRequest());
        List<FlightOffer> second = engine.findBestOffers(sampleRequest());
        assertThat(first).isEqualTo(second);
    }

    private SearchRequest withPrecision(SearchPrecision precision) {
        return new SearchRequest("BCN", "NRT",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30),
                14, 3, 1, 5, precision);
    }

    @Test
    void fastHaceMenosConsultasQueExhaustive() {
        // Provider que cuenta cuántas veces se le llama de verdad.
        AtomicInteger callsFast = new AtomicInteger();
        AtomicInteger callsExhaustive = new AtomicInteger();

        var fastEngine = new SlidingWindowEngine(countingProvider(callsFast), CONCURRENCY);
        var exhaustiveEngine = new SlidingWindowEngine(countingProvider(callsExhaustive), CONCURRENCY);

        fastEngine.findBestOffers(withPrecision(SearchPrecision.FAST));
        exhaustiveEngine.findBestOffers(withPrecision(SearchPrecision.EXHAUSTIVE));

        // FAST salta de 3 en 3 días → en torno a un tercio de las consultas.
        assertThat(callsFast.get()).isLessThan(callsExhaustive.get());
        assertThat(callsFast.get()).isCloseTo(callsExhaustive.get() / 3, withinPercentage(20));
    }

    @Test
    void respetaElPrecioMaximoSiSeIndica() {
        BigDecimal budget = new BigDecimal("400");
        SearchRequest req = new SearchRequest("BCN", "NRT",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30),
                14, 3, 1, 10, SearchPrecision.EXHAUSTIVE, budget);

        List<FlightOffer> result = engine.findBestOffers(req);

        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(offer ->
                assertThat(offer.price()).isLessThanOrEqualTo(budget));
    }

    @Test
    void cualquierPrecisionRespetaTopNyOrden() {
        for (SearchPrecision p : SearchPrecision.values()) {
            List<FlightOffer> result = engine.findBestOffers(withPrecision(p));
            assertThat(result).as("precisión %s", p)
                    .isNotEmpty()
                    .hasSizeLessThanOrEqualTo(5)
                    .isSortedAccordingTo((a, b) -> a.price().compareTo(b.price()));
        }
    }

    /** FlightProvider que delega en el mock pero cuenta las llamadas. */
    private FlightProvider countingProvider(AtomicInteger counter) {
        MockFlightProvider mock = new MockFlightProvider();
        return (origin, destination, depart, ret, maxStops) -> {
            counter.incrementAndGet();
            return mock.searchOffers(origin, destination, depart, ret, maxStops);
        };
    }
}
