package com.torii.cache;

import com.torii.config.CacheProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Decides how long a trip price stays cached based on how close the departure is.
 *
 * <p>This is the main idea behind the cache: a flight 6 months out barely changes
 * price from one day to the next, so it can be kept for a long time. A flight in 3
 * days can move within hours, so it should be refreshed soon.
 *
 * <p>Thresholds and durations come from {@link CacheProperties}
 * ({@code torii.cache.ttl} prefix), so they can be tuned without recompiling.
 *
 * <p>Still <b>pure logic</b>: takes "today" as a parameter and returns a
 * {@link Duration}, which makes it trivial to test.
 */
@Component
public class TripTtlPolicy {

    private final CacheProperties.Ttl ttl;

    public TripTtlPolicy(CacheProperties properties) {
        this.ttl = properties.ttl();
    }

    /**
     * TTL for a trip departing on {@code departDate}, seen from {@code today}, based
     * on the configured tiers.
     */
    public Duration ttlFor(LocalDate departDate, LocalDate today) {
        long daysUntilDeparture = ChronoUnit.DAYS.between(today, departDate);

        if (daysUntilDeparture > ttl.farThresholdDays()) {
            return ttl.far();
        } else if (daysUntilDeparture >= ttl.mediumThresholdDays()) {
            return ttl.medium();
        } else if (daysUntilDeparture >= ttl.nearThresholdDays()) {
            return ttl.near();
        } else {
            return ttl.imminent();
        }
    }
}
