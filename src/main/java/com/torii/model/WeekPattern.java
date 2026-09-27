package com.torii.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * "Getaway" filter: only explores trips that leave and come back on specific days of
 * the week (the typical case is Friday to Sunday).
 *
 * <p>When a {@link SearchRequest} has a pattern, it <b>replaces</b>
 * {@code baseDuration} / {@code variability}: the length of stay is the distance
 * between the two weekdays. Friday to Sunday is 2 nights, Friday to Monday is 3.
 *
 * <p>It also cuts the number of lookups a lot: one per week instead of one per day in
 * the range.
 */
public record WeekPattern(DayOfWeek departDay, DayOfWeek returnDay) {

    public WeekPattern {
        if (departDay == null || returnDay == null) {
            throw new IllegalArgumentException("El patrón semanal necesita día de salida y de vuelta");
        }
    }

    /**
     * Nights of stay the pattern implies.
     *
     * <p>Counted forward through the week: Friday to Sunday is 2 days. If both days
     * are the same (e.g. Friday to Friday) it means a full week, 7 days, not a 0 day
     * trip.
     */
    public int stayDays() {
        int diff = Math.floorMod(returnDay.getValue() - departDay.getValue(), 7);
        return diff == 0 ? 7 : diff;
    }

    /** First date on or after {@code from} that falls on the departure day. */
    public LocalDate firstDeparture(LocalDate from) {
        int diff = Math.floorMod(departDay.getValue() - from.getDayOfWeek().getValue(), 7);
        return from.plusDays(diff);
    }
}
