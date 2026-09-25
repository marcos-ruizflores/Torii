package com.torii.provider;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.torii.cache.TripTtlPolicy;
import com.torii.model.FlightOffer;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Decorator that puts a Caffeine cache in front of ANOTHER {@link FlightProvider}.
 *
 * <p>It implements the same interface it wraps, so for the rest of the system it's
 * just one more FlightProvider. Neither the algorithm nor the real providers know
 * the cache is there.
 *
 * <p>The cache key is the exact date pair, so the same outbound/return days get
 * reused across different searches. Each entry's TTL comes from
 * {@link TripTtlPolicy} based on how close the departure date is. Since the TTL is
 * per entry, this uses Caffeine directly instead of @Cacheable.
 */
public class CachingFlightProvider implements FlightProvider {

    /** Cache key: uniquely identifies one lookup against the source. */
    private record CacheKey(
            String origin,
            String destination,
            LocalDate departDate,
            LocalDate returnDate,
            int maxStops
    ) {}

    private final FlightProvider delegate;
    private final Clock clock;
    private final Cache<CacheKey, List<FlightOffer>> cache;

    public CachingFlightProvider(FlightProvider delegate, TripTtlPolicy ttlPolicy,
                                 Clock clock, long maximumSize) {
        this.delegate = delegate;
        this.clock = clock;
        this.cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)   // configurable cap, evicts the least used entries
                .recordStats()              // track hits/misses so we can see if it's worth it
                .expireAfter(new TripExpiry(ttlPolicy))
                .build();
    }

    @Override
    public List<FlightOffer> searchOffers(
            String origin, String destination,
            LocalDate departDate, LocalDate returnDate, int maxStops) {

        CacheKey key = new CacheKey(origin, destination, departDate, returnDate, maxStops);

        // get(key, mappingFunction) returns the cached value, or calls the delegate,
        // stores the result and returns it. It's atomic, so two threads asking for
        // the same key won't trigger two real calls.
        return cache.get(key, k ->
                delegate.searchOffers(origin, destination, departDate, returnDate, maxStops));
    }

    /** Cache stats (hits, misses, size) for monitoring. */
    public CacheStats stats() {
        return cache.stats();
    }

    public long estimatedSize() {
        return cache.estimatedSize();
    }

    /**
     * Adapts {@link TripTtlPolicy} to Caffeine's Expiry contract. The TTL is set when
     * the entry is CREATED and never extended on read or update: we want prices to be
     * refreshed after a while, not kept alive just because they're popular.
     */
    private final class TripExpiry implements Expiry<CacheKey, List<FlightOffer>> {

        private final TripTtlPolicy ttlPolicy;

        private TripExpiry(TripTtlPolicy ttlPolicy) {
            this.ttlPolicy = ttlPolicy;
        }

        @Override
        public long expireAfterCreate(CacheKey key, List<FlightOffer> value, long currentTime) {
            return ttlPolicy.ttlFor(key.departDate(), LocalDate.now(clock)).toNanos();
        }

        @Override
        public long expireAfterUpdate(CacheKey key, List<FlightOffer> value,
                                      long currentTime, long currentDuration) {
            return currentDuration; // keep the TTL we already computed
        }

        @Override
        public long expireAfterRead(CacheKey key, List<FlightOffer> value,
                                    long currentTime, long currentDuration) {
            return currentDuration; // reads don't extend the lifetime
        }
    }
}
