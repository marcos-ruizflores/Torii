package com.torii.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Petición de búsqueda ya validada y lista para el dominio.
 *
 * <p>Representa lo que el usuario pide a Torii:
 * <ul>
 *   <li>{@code origin} / {@code destination}: códigos IATA (ej. "BCN", "NRT").</li>
 *   <li>{@code rangeStart} / {@code rangeEnd}: la ventana de vacaciones posible
 *       (ej. 1 de julio – 30 de septiembre).</li>
 *   <li>{@code baseDuration}: días que se quiere estar (ej. 14).</li>
 *   <li>{@code variability}: cuánto se permite alargar la estancia. Con base 14 y
 *       variabilidad 3 se exploran duraciones de 14, 15, 16 y 17 días.</li>
 *   <li>{@code maxStops}: número máximo de escalas aceptadas.</li>
 *   <li>{@code topN}: cuántas mejores ofertas devolver.</li>
 *   <li>{@code precision}: granularidad de la exploración (ver {@link SearchPrecision}).</li>
 *   <li>{@code maxPrice}: presupuesto máximo opcional ({@code null} = sin límite). Se
 *       aplica como filtro en el algoritmo, NO en la llamada a la API, para que la
 *       caché siga siendo reutilizable entre distintos presupuestos.</li>
 * </ul>
 *
 * <p>Es un {@code record}: inmutable y sin lógica. Las reglas de validación viven
 * en el DTO de entrada ({@code SearchRequestDto}), de modo que el dominio siempre
 * trabaja con datos ya correctos.
 */
public record SearchRequest(
        String origin,
        String destination,
        LocalDate rangeStart,
        LocalDate rangeEnd,
        int baseDuration,
        int variability,
        int maxStops,
        int topN,
        SearchPrecision precision,
        BigDecimal maxPrice
) {
    /**
     * Constructor de conveniencia con precisión {@link SearchPrecision#EXHAUSTIVE}
     * por defecto y sin límite de precio. Mantiene compatibilidad con el código (y
     * tests) anterior a esos campos.
     */
    public SearchRequest(String origin, String destination,
                         LocalDate rangeStart, LocalDate rangeEnd,
                         int baseDuration, int variability, int maxStops, int topN) {
        this(origin, destination, rangeStart, rangeEnd,
                baseDuration, variability, maxStops, topN, SearchPrecision.EXHAUSTIVE, null);
    }

    /** Constructor de conveniencia con precisión explícita y sin límite de precio. */
    public SearchRequest(String origin, String destination,
                         LocalDate rangeStart, LocalDate rangeEnd,
                         int baseDuration, int variability, int maxStops, int topN,
                         SearchPrecision precision) {
        this(origin, destination, rangeStart, rangeEnd,
                baseDuration, variability, maxStops, topN, precision, null);
    }

    /** Duración mínima a explorar (la base). */
    public int minDuration() {
        return baseDuration;
    }

    /** Duración máxima a explorar (base + variabilidad). */
    public int maxDuration() {
        return baseDuration + variability;
    }
}
