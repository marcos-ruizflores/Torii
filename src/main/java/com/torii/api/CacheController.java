package com.torii.api;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.torii.provider.CachingFlightProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Cache monitoring endpoint: {@code GET /api/cache/stats}.
 *
 * <p>Lets you actually see the cache working. After a search lots of date pairs are
 * cached, so repeating an overlapping search bumps the hits while real calls to the
 * source (misses) stay flat. Every miss is a call that would count against a real
 * provider's monthly quota.
 */
@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private final CachingFlightProvider cachingProvider;

    public CacheController(CachingFlightProvider cachingProvider) {
        this.cachingProvider = cachingProvider;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        CacheStats s = cachingProvider.stats();
        return Map.of(
                "entradasEnCache", cachingProvider.estimatedSize(),
                "hits", s.hitCount(),
                "misses", s.missCount(),          // = real calls to the source
                "hitRate", String.format("%.1f%%", s.hitRate() * 100),
                "evictions", s.evictionCount()
        );
    }
}
