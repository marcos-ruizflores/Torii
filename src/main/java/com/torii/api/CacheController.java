package com.torii.api;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.torii.provider.CachingFlightProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de observabilidad de la caché: {@code GET /api/cache/stats}.
 *
 * <p>Sirve para "ver" el efecto del paso 2. Tras una búsqueda, muchos pares de
 * fechas quedan cacheados; al repetir una búsqueda solapada, los aciertos (hits)
 * suben y las llamadas reales a la fuente (misses) no. En cada miss es donde, con
 * Amadeus, se gastaría una llamada de la cuota mensual.
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
                "misses", s.missCount(),          // = llamadas reales a la fuente
                "hitRate", String.format("%.1f%%", s.hitRate() * 100),
                "evictions", s.evictionCount()
        );
    }
}
