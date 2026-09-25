package com.torii.api;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.torii.auth.SecurityConfig;
import com.torii.config.CorsConfig;
import com.torii.provider.CachingFlightProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer test for the cache stats endpoint. Mocks {@link CachingFlightProvider}
 * to check its metrics come out right in the JSON of {@code GET /api/cache/stats}.
 */
@WebMvcTest(CacheController.class)
// Use the real security chain (where /api/cache is public) instead of the default
// one, which would require auth everywhere.
@Import({SecurityConfig.class, CorsConfig.class})
class CacheControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CachingFlightProvider cachingProvider;

    @Test
    void exponeLasEstadisticasDeLaCache() throws Exception {
        when(cachingProvider.estimatedSize()).thenReturn(42L);
        // CacheStats.of(hits, misses, loadSuccess, loadFailure, totalLoadTime, evictions, evictionWeight)
        when(cachingProvider.stats()).thenReturn(CacheStats.of(10, 5, 0, 0, 0, 3, 0));

        mvc.perform(get("/api/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entradasEnCache").value(42))
                .andExpect(jsonPath("$.hits").value(10))
                .andExpect(jsonPath("$.misses").value(5))
                .andExpect(jsonPath("$.evictions").value(3));
    }
}
