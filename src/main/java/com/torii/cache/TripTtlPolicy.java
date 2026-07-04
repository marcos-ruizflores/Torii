package com.torii.cache;

import com.torii.config.CacheProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Decide cuánto tiempo vive en caché el precio de un viaje según lo cerca que esté
 * la fecha de salida.
 *
 * <p>Es la idea central de la caché de Torii: el precio de un vuelo a 6 meses vista
 * apenas cambia de un día para otro, así que podemos guardarlo mucho tiempo; pero el
 * precio de un vuelo dentro de 3 días puede moverse en horas, así que conviene
 * refrescarlo pronto.
 *
 * <p>Los umbrales y duraciones ya no están escritos a fuego: vienen de
 * {@link CacheProperties} (prefijo {@code torii.cache.ttl}), de modo que se pueden
 * ajustar sin recompilar.
 *
 * <p>Sigue siendo <b>lógica pura</b>: recibe "hoy" como parámetro y devuelve una
 * {@link Duration}, lo que la hace trivial de testear.
 */
@Component
public class TripTtlPolicy {

    private final CacheProperties.Ttl ttl;

    public TripTtlPolicy(CacheProperties properties) {
        this.ttl = properties.ttl();
    }

    /**
     * Calcula el TTL para un viaje cuya salida es {@code departDate}, visto desde
     * {@code today}, según los tramos configurados.
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
