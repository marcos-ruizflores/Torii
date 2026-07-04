package com.torii.history;

import com.torii.model.FlightOffer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// Spring Boot 4: las anotaciones de test por capas viven en módulos propios.
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del histórico de precios contra la H2 en memoria (con las migraciones de
 * Flyway aplicadas, como en producción). Verifica el "upsert" del mejor precio del
 * día y que las ofertas del mock no contaminan el histórico.
 */
@DataJpaTest
// No sustituyas mi datasource: usa la H2 modo-PostgreSQL de application.properties de test.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PriceHistoryService.class)
class PriceHistoryServiceTest {

    /** Reloj fijo: los tests no dependen de la hora real. */
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 4);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(
                    Instant.parse("2026-07-04T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private PriceHistoryService service;

    @Autowired
    private PriceHistoryRepository repository;

    private static FlightOffer offer(String price, String bookingUrl) {
        return new FlightOffer("Iberia", new BigDecimal(price), "EUR", 0,
                TODAY.plusMonths(2), TODAY.plusMonths(2).plusDays(14), bookingUrl);
    }

    @Test
    void guardaLaMejorOfertaDelDia() {
        service.recordObservation("BCN", "NRT", List.of(
                offer("900.00", "https://www.skyscanner.net/x"),
                offer("750.50", "https://www.skyscanner.net/y")));

        PriceHistoryEntry entry = repository
                .findByOriginAndDestinationAndDay("BCN", "NRT", TODAY).orElseThrow();
        assertThat(entry.getBestPrice()).isEqualByComparingTo("750.50");
        assertThat(entry.getProvider()).isEqualTo("FlightAPI");
    }

    @Test
    void soloActualizaSiElNuevoPrecioEsMasBarato() {
        service.recordObservation("BCN", "NRT", List.of(offer("800.00", "https://www.skyscanner.net/x")));
        // Segunda búsqueda del mismo día, más cara: NO debe pisar el 800.
        service.recordObservation("BCN", "NRT", List.of(offer("850.00", "https://www.skyscanner.net/x")));
        // Tercera, más barata: SÍ actualiza.
        service.recordObservation("BCN", "NRT", List.of(offer("790.00", "https://www.skyscanner.net/x")));

        PriceHistoryEntry entry = repository
                .findByOriginAndDestinationAndDay("BCN", "NRT", TODAY).orElseThrow();
        assertThat(entry.getBestPrice()).isEqualByComparingTo("790.00");
        assertThat(repository.count()).isEqualTo(1); // sigue habiendo UNA fila por (ruta, día)
    }

    @Test
    void ignoraLasOfertasDelMock() {
        service.recordObservation("BCN", "MAD", List.of(
                offer("120.00", "https://example.com/booking?x=1")));

        assertThat(repository.count()).isZero();
    }

    @Test
    void devuelveLaSerieOrdenadaYLimitadaPorDias() {
        repository.save(new PriceHistoryEntry("BCN", "NRT", TODAY.minusDays(40),
                new BigDecimal("700.00"), "EUR", null)); // fuera de la ventana de 30 días
        repository.save(new PriceHistoryEntry("BCN", "NRT", TODAY.minusDays(3),
                new BigDecimal("810.00"), "EUR", null));
        repository.save(new PriceHistoryEntry("BCN", "NRT", TODAY.minusDays(1),
                new BigDecimal("795.00"), "EUR", null));

        List<PricePointDto> serie = service.history("BCN", "NRT", 30);

        assertThat(serie).hasSize(2);
        assertThat(serie.get(0).date()).isEqualTo(TODAY.minusDays(3));
        assertThat(serie.get(1).price()).isEqualByComparingTo("795.00");
    }
}
