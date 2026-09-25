package com.torii.cache;

import com.torii.config.CacheProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the variable TTL policy. It's pure logic, so no Spring or cache needed:
 * pin "today" and check the TTL for departures at different distances.
 */
class TripTtlPolicyTest {

    private final TripTtlPolicy policy = new TripTtlPolicy(CacheProperties.defaults());
    private final LocalDate today = LocalDate.of(2026, 1, 1);

    @Test
    void viajeMuyLejanoTtlLargo() {
        // departure 6 months out -> 7 days
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
        // exactly 60 days -> not above 60 yet, falls in the 24h tier
        assertThat(policy.ttlFor(today.plusDays(60), today)).isEqualTo(Duration.ofHours(24));
        // exactly 61 -> now it's "far"
        assertThat(policy.ttlFor(today.plusDays(61), today)).isEqualTo(Duration.ofDays(7));
        // exactly 14 -> 24h tier
        assertThat(policy.ttlFor(today.plusDays(14), today)).isEqualTo(Duration.ofHours(24));
    }
}
