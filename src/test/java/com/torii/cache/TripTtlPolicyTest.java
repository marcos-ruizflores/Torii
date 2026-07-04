package com.torii.cache;

import com.torii.config.CacheProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la política de TTL variable. Al ser lógica pura, se prueban sin Spring ni
 * caché: fijamos un "hoy" y comprobamos el TTL para distintas cercanías de salida.
 */
class TripTtlPolicyTest {

    private final TripTtlPolicy policy = new TripTtlPolicy(CacheProperties.defaults());
    private final LocalDate today = LocalDate.of(2026, 1, 1);

    @Test
    void viajeMuyLejanoTtlLargo() {
        // salida a 6 meses → 7 días
        assertThat(policy.ttlFor(today.plusDays(180), today)).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void viajeAMedioPlazoTtl24h() {
        assertThat(policy.ttlFor(today.plusDays(30), today)).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void viajeCercanoTtl1h() {
        assertThat(policy.ttlFor(today.plusDays(5), today)).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void viajeInminenteTtlCorto() {
        assertThat(policy.ttlFor(today.plusDays(1), today)).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void enLosLimitesElTtlEsElDelTramoSuperior() {
        // exactamente 60 días → todavía no supera 60, cae en el tramo de 24h
        assertThat(policy.ttlFor(today.plusDays(60), today)).isEqualTo(Duration.ofHours(24));
        // exactamente 61 → ya es "muy lejano"
        assertThat(policy.ttlFor(today.plusDays(61), today)).isEqualTo(Duration.ofDays(7));
        // exactamente 14 → tramo de 24h
        assertThat(policy.ttlFor(today.plusDays(14), today)).isEqualTo(Duration.ofHours(24));
    }
}
