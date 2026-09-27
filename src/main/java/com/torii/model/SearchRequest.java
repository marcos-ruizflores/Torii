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
 *   <li>{@code weekPattern}: optional getaway filter ({@code null} means no filter).
 *       When set, only trips leaving and returning on those days of the week are
 *       explored (e.g. Friday to Sunday) and {@code baseDuration} and
 *       {@code variability} are <b>ignored</b>: the pattern sets the length.</li>
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
        BigDecimal maxPrice,
        WeekPattern weekPattern
) {
    /**
     * Convenience constructor: {@link SearchPrecision#EXHAUSTIVE} precision and no
     * price limit. Keeps code and tests written before those fields compiling.
     */
    public SearchRequest(String origin, String destination,
                         LocalDate rangeStart, LocalDate rangeEnd,
                         int baseDuration, int variability, int maxStops, int topN) {
        this(origin, destination, rangeStart, rangeEnd,
                baseDuration, variability, maxStops, topN, SearchPrecision.EXHAUSTIVE, null, null);
    }

    /** Convenience constructor with an explicit precision and no price limit. */
    public SearchRequest(String origin, String destination,
                         LocalDate rangeStart, LocalDate rangeEnd,
                         int baseDuration, int variability, int maxStops, int topN,
                         SearchPrecision precision) {
        this(origin, destination, rangeStart, rangeEnd,
                baseDuration, variability, maxStops, topN, precision, null, null);
    }

    /** Convenience constructor without the getaway filter (weekly pattern). */
    public SearchRequest(String origin, String destination,
                         LocalDate rangeStart, LocalDate rangeEnd,
                         int baseDuration, int variability, int maxStops, int topN,
                         SearchPrecision precision, BigDecimal maxPrice) {
        this(origin, destination, rangeStart, rangeEnd,
                baseDuration, variability, maxStops, topN, precision, maxPrice, null);
    }

    /** Same request at another precision (the plan can decide it after validation). */
    public SearchRequest withPrecision(SearchPrecision newPrecision) {
        return new SearchRequest(origin, destination, rangeStart, rangeEnd, baseDuration, variability,
                maxStops, topN, newPrecision, maxPrice, weekPattern);
    }

    /** Is this a weekend getaway search (or whatever weekly pattern it uses)? */
    public boolean hasWeekPattern() {
        return weekPattern != null;
    }

    /** Shortest trip length to explore (the base, or whatever the weekly pattern sets). */
    public int minDuration() {
        return hasWeekPattern() ? weekPattern.stayDays() : baseDuration;
    }

    /** Longest trip length to explore (base + variability, or the same as the shortest with a pattern). */
    public int maxDuration() {
        return hasWeekPattern() ? weekPattern.stayDays() : baseDuration + variability;
    }
}
