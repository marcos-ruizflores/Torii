package com.torii.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Cache settings, externalized to {@code application.properties} under the
 * {@code torii.cache} prefix.
 *
 * <p>Cache size and TTLs used to be hardcoded. Having them here means they can be
 * tuned without recompiling (per environment, profile or env var) and there's one
 * single place to see and change all those numbers.
 *
 * <p>Constructor-bound {@code record}: Spring fills each field from the properties
 * and falls back to the {@link DefaultValue} when one is missing, so the app works
 * even with no config at all.
 *
 * <p>Durations use a suffix: {@code 7d}, {@code 24h}, {@code 1h}, {@code 10m}. Spring
 * converts them to {@link Duration} on its own.
 */
@ConfigurationProperties(prefix = "torii.cache")
public record CacheProperties(

        /** Max number of cache entries. Least used ones get evicted past this. */
        @DefaultValue("50000") long maximumSize,

        /** Expiry policy, depends on how soon the trip is. */
        @DefaultValue Ttl ttl
) {

    /**
     * TTL tiers. {@code *ThresholdDays} are the boundaries in days until departure,
     * the other fields are how long an entry lives in each tier.
     */
    public record Ttl(
            @DefaultValue("60") int farThresholdDays,
            @DefaultValue("14") int mediumThresholdDays,
            @DefaultValue("2")  int nearThresholdDays,

            @DefaultValue("7d")  Duration far,       // departure > farThresholdDays away
            @DefaultValue("24h") Duration medium,    // >= mediumThresholdDays
            @DefaultValue("1h")  Duration near,      // >= nearThresholdDays
            @DefaultValue("10m") Duration imminent   // < nearThresholdDays (or already past)
    ) {}

    /**
     * Instance with every default value. Useful in tests that don't start the Spring
     * context and need to build the properties by hand.
     */
    public static CacheProperties defaults() {
        return new CacheProperties(
                50_000,
                new Ttl(60, 14, 2,
                        Duration.ofDays(7), Duration.ofHours(24),
                        Duration.ofHours(1), Duration.ofMinutes(10)));
    }
}
