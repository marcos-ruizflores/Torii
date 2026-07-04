package com.torii.provider;

import com.torii.model.FlightOffer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del motor de failover. Usan proveedores simulados que cuentan sus llamadas y
 * un reloj manipulable, para comprobar el comportamiento exacto de conmutación y
 * enfriamiento sin depender de ninguna API real.
 */
class FailoverFlightProviderTest {

    private static final Duration COOLDOWN = Duration.ofMinutes(5);
    private static final LocalDate DEPART = LocalDate.of(2026, 7, 1);
    private static final LocalDate RETURN = LocalDate.of(2026, 7, 15);

    private List<FlightOffer> search(FailoverFlightProvider failover) {
        return failover.searchOffers("BCN", "NRT", DEPART, RETURN, 1);
    }

    @Test
    void usaElPrimeroSiResponde() {
        var primero = new RespondingProvider("Primero");
        var segundo = new RespondingProvider("Segundo");
        var failover = new FailoverFlightProvider(List.of(primero, segundo),
                fixedClock(), COOLDOWN);

        List<FlightOffer> result = search(failover);

        assertThat(result).hasSize(1);
        assertThat(primero.calls).isEqualTo(1);
        assertThat(segundo.calls).isZero(); // ni se toca
    }

    @Test
    void saltaAlSiguienteCuandoElPrimeroSeAgota() {
        var agotado = new QuotaExhaustedProvider("Amadeus");
        var responde = new RespondingProvider("Google");
        var failover = new FailoverFlightProvider(List.of(agotado, responde),
                fixedClock(), COOLDOWN);

        List<FlightOffer> result = search(failover);

        assertThat(result).hasSize(1);
        assertThat(agotado.calls).isEqualTo(1);
        assertThat(responde.calls).isEqualTo(1);
    }

    @Test
    void noReintentaUnProveedorAgotadoDentroDelCooldown() {
        var agotado = new QuotaExhaustedProvider("Amadeus");
        var responde = new RespondingProvider("Google");
        var failover = new FailoverFlightProvider(List.of(agotado, responde),
                fixedClock(), COOLDOWN);

        // Dos búsquedas seguidas (simula dos de los cientos de pares de fechas).
        search(failover);
        search(failover);

        // La primera lo intentó y lo aparcó; la segunda lo saltó directamente.
        assertThat(agotado.calls).isEqualTo(1);
        assertThat(responde.calls).isEqualTo(2);
    }

    @Test
    void vuelveAIntentarElProveedorTrasElCooldown() {
        var agotado = new QuotaExhaustedProvider("Amadeus");
        var responde = new RespondingProvider("Google");
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        var failover = new FailoverFlightProvider(List.of(agotado, responde), clock, COOLDOWN);

        search(failover);                       // agota y aparca Amadeus
        clock.advance(Duration.ofMinutes(6));   // pasa el cooldown de 5 min
        search(failover);                       // debería reintentar Amadeus

        assertThat(agotado.calls).isEqualTo(2); // se reintentó tras enfriarse
    }

    @Test
    void lanzaSiNingunProveedorPuedeAtender() {
        var agotado = new QuotaExhaustedProvider("Amadeus");
        var caido = new TransientFailProvider("Google");
        var failover = new FailoverFlightProvider(List.of(agotado, caido),
                fixedClock(), COOLDOWN);

        assertThatThrownBy(() -> search(failover))
                .isInstanceOf(FlightProviderException.class)
                .hasMessageContaining("Ningún proveedor");
    }

    // --- Proveedores simulados ---------------------------------------------------

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    }

    /** Responde siempre con una oferta. */
    private static class RespondingProvider implements FlightProvider {
        private final String name;
        int calls = 0;

        RespondingProvider(String name) {
            this.name = name;
        }

        @Override
        public List<FlightOffer> searchOffers(String o, String d, LocalDate dep, LocalDate ret, int s) {
            calls++;
            return List.of(new FlightOffer(name, new BigDecimal("100.00"), "EUR", 0, dep, ret, "url"));
        }

        @Override
        public String name() {
            return name;
        }
    }

    /** Siempre lanza "cuota agotada". */
    private static class QuotaExhaustedProvider implements FlightProvider {
        private final String name;
        int calls = 0;

        QuotaExhaustedProvider(String name) {
            this.name = name;
        }

        @Override
        public List<FlightOffer> searchOffers(String o, String d, LocalDate dep, LocalDate ret, int s) {
            calls++;
            throw new ProviderQuotaExceededException(name + " sin cuota");
        }

        @Override
        public String name() {
            return name;
        }
    }

    /** Siempre lanza un fallo temporal. */
    private static class TransientFailProvider implements FlightProvider {
        private final String name;
        int calls = 0;

        TransientFailProvider(String name) {
            this.name = name;
        }

        @Override
        public List<FlightOffer> searchOffers(String o, String d, LocalDate dep, LocalDate ret, int s) {
            calls++;
            throw new FlightProviderException(name + " timeout");
        }

        @Override
        public String name() {
            return name;
        }
    }

    /** Reloj cuyo instante podemos avanzar a mano, para probar el cooldown. */
    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration d) {
            instant = instant.plus(d);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
