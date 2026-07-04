package com.torii.provider;

import com.torii.cache.TripTtlPolicy;
import com.torii.config.CacheProperties;
import com.torii.model.FlightOffer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del decorador de caché. Usamos un delegate "espía" que cuenta cuántas veces
 * se le llama de verdad, para comprobar que la caché ahorra esas llamadas.
 */
class CachingFlightProviderTest {

    private final Clock fixedClock =
            Clock.fixed(LocalDate.of(2026, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    ZoneId.systemDefault());

    /** FlightProvider falso que cuenta llamadas y devuelve siempre una oferta. */
    private static class CountingProvider implements FlightProvider {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public List<FlightOffer> searchOffers(String origin, String destination,
                                              LocalDate departDate, LocalDate returnDate, int maxStops) {
            calls.incrementAndGet();
            return List.of(new FlightOffer("TestAir", new BigDecimal("100.00"), "EUR",
                    0, departDate, returnDate, "https://test"));
        }
    }

    @Test
    void segundaLlamadaIdenticaNoTocaLaFuente() {
        CountingProvider source = new CountingProvider();
        CachingFlightProvider caching = new CachingFlightProvider(
                source, new TripTtlPolicy(CacheProperties.defaults()), fixedClock, 50_000);

        LocalDate depart = LocalDate.of(2026, 7, 1);
        LocalDate ret = LocalDate.of(2026, 7, 15);

        List<FlightOffer> first = caching.searchOffers("BCN", "NRT", depart, ret, 1);
        List<FlightOffer> second = caching.searchOffers("BCN", "NRT", depart, ret, 1);

        assertThat(source.calls.get()).isEqualTo(1);   // solo una llamada real
        assertThat(second).isEqualTo(first);            // mismo resultado
        assertThat(caching.stats().hitCount()).isEqualTo(1);
        assertThat(caching.stats().missCount()).isEqualTo(1);
    }

    @Test
    void clavesDistintasProvocanLlamadasDistintas() {
        CountingProvider source = new CountingProvider();
        CachingFlightProvider caching = new CachingFlightProvider(
                source, new TripTtlPolicy(CacheProperties.defaults()), fixedClock, 50_000);

        caching.searchOffers("BCN", "NRT", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15), 1);
        caching.searchOffers("BCN", "NRT", LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 16), 1);

        assertThat(source.calls.get()).isEqualTo(2);   // fechas distintas → dos llamadas
    }
}
