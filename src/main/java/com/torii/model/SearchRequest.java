package com.torii.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A validated search request, ready for the domain layer.
 *
 * <p>What the user is asking Torii for:
 * <ul>
 *   <li>{@code origin} / {@code destination}: IATA codes (e.g. "BCN", "NRT").</li>
 *   <li>{@code rangeStart} / {@code rangeEnd}: the possible holiday window
 *       (e.g. July 1 to September 30).</li>
 *   <li>{@code baseDuration}: how many days the user wants to stay (e.g. 14).</li>
 *   <li>{@code variability}: how much longer the stay can be. Base 14 with
 *       variability 3 explores 14, 15, 16 and 17 day trips.</li>
 *   <li>{@code maxStops}: max number of stops allowed.</li>
 *   <li>{@code topN}: how many offers to return.</li>
 *   <li>{@code precision}: how fine-grained the search is (see {@link SearchPrecision}).</li>
 *   <li>{@code maxPrice}: optional budget ({@code null} means no limit). It's applied
 *       as a filter in the algorithm, NOT in the API call, so the cache can still be
 *       reused across different budgets.</li>
 * </ul>
 *
 * <p>Plain immutable {@code record} with no logic. Validation lives in the input DTO
 * ({@code SearchRequestDto}), so the domain always gets clean data.
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
     * Convenience constructor: {@link SearchPrecision#EXHAUSTIVE} precision and no
     * price limit. Keeps code and tests written before those fields compiling.
     */
    public SearchRequest(String origin, String destination,
                         LocalDate rangeStart, LocalDate rangeEnd,
                         int baseDuration, int variability, int maxStops, int topN) {
        this(origin, destination, rangeStart, rangeEnd,
                baseDuration, variability, maxStops, topN, SearchPrecision.EXHAUSTIVE, null);
    }

    /** Convenience constructor with an explicit precision and no price limit. */
    public SearchRequest(String origin, String destination,
                         LocalDate rangeStart, LocalDate rangeEnd,
                         int baseDuration, int variability, int maxStops, int topN,
                         SearchPrecision precision) {
        this(origin, destination, rangeStart, rangeEnd,
                baseDuration, variability, maxStops, topN, precision, null);
    }

    /** Shortest trip length to explore (the base). */
    public int minDuration() {
        return baseDuration;
    }

    /** Longest trip length to explore (base + variability). */
    public int maxDuration() {
        return baseDuration + variability;
    }
}
