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
 * Decorador que añade una caché Caffeine por delante de OTRO {@link FlightProvider}.
 *
 * <p>Patrón decorador: implementa la misma interfaz que envuelve, así que para el
 * resto del sistema es "un FlightProvider más". Hoy envuelve al
 * {@link MockFlightProvider}; el día que tengamos {@code AmadeusFlightProvider} lo
 * envolverá a él sin cambiar nada aquí. Ni el algoritmo ni Amadeus saben que la
 * caché existe.
 *
 * <p>La clave de caché es el par de fechas concreto, de modo que el mismo día de
 * salida/vuelta se reutiliza entre búsquedas distintas. El TTL de cada entrada lo
 * decide {@link TripTtlPolicy} según la cercanía de la fecha de salida (caducidad
 * variable por entrada — por eso usamos Caffeine directamente y no @Cacheable).
 */
public class CachingFlightProvider implements FlightProvider {

    /** Clave de caché: identifica unívocamente una consulta a la fuente. */
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
                .maximumSize(maximumSize)   // tope de entradas (configurable); expulsa las menos usadas
                .recordStats()              // habilita hits/misses para poder observar el efecto
                .expireAfter(new TripExpiry(ttlPolicy))
                .build();
    }

    @Override
    public List<FlightOffer> searchOffers(
            String origin, String destination,
            LocalDate departDate, LocalDate returnDate, int maxStops) {

        CacheKey key = new CacheKey(origin, destination, departDate, returnDate, maxStops);

        // get(key, mappingFunction): si está en caché lo devuelve; si no, ejecuta la
        // función (llamada real al delegate), guarda el resultado y lo devuelve.
        // Es atómico: dos hilos pidiendo la misma clave no disparan dos llamadas.
        return cache.get(key, k ->
                delegate.searchOffers(origin, destination, departDate, returnDate, maxStops));
    }

    /** Estadísticas de la caché (hits, misses, tamaño) para observabilidad. */
    public CacheStats stats() {
        return cache.stats();
    }

    public long estimatedSize() {
        return cache.estimatedSize();
    }

    /**
     * Traduce la {@link TripTtlPolicy} al contrato de caducidad de Caffeine.
     * El TTL se fija al CREAR la entrada y no se renueva al leerla ni actualizarla
     * (queremos refrescar el precio pasado su tiempo, no mantenerlo vivo por uso).
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
            return currentDuration; // conservar el TTL ya calculado
        }

        @Override
        public long expireAfterRead(CacheKey key, List<FlightOffer> value,
                                    long currentTime, long currentDuration) {
            return currentDuration; // leer no prolonga la vida
        }
    }
}
