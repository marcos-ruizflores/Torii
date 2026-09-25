package com.torii.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * A single round-trip offer for a specific pair of dates.
 *
 * <p>It's what a {@link com.torii.provider.FlightProvider} returns for an
 * (origin, destination, outbound date, return date) combination, and also what the
 * algorithm picks as the best results.
 *
 * <p>Design notes:
 * <ul>
 *   <li>Price is a {@link BigDecimal}. Never use {@code double} for money, it
 *       introduces rounding errors.</li>
 *   <li>Trip length isn't stored as a field. It's derived from the dates in
 *       {@link #durationDays()} so there's no redundant data that could get out of
 *       sync.</li>
 *   <li>{@code departureTime}, {@code returnDepartureTime} and {@code stopovers} are
 *       there to tell apart flights that would otherwise look the same (same airline
 *       and price, different times or stops). They can be missing ({@code null} or an
 *       empty list) when the source doesn't provide them, e.g. SerpApi doesn't give
 *       the return time without a second call.</li>
 * </ul>
 */
public record FlightOffer(
        String airline,
        BigDecimal price,
        String currency,
        int stops,
        LocalDate departDate,
        LocalDate returnDate,
        LocalTime departureTime,
        LocalTime returnDepartureTime,
        List<String> stopovers,
        String bookingUrl
) {
    /**
     * Convenience constructor without times and stopovers, for code and tests that
     * don't need them.
     */
    public FlightOffer(String airline, BigDecimal price, String currency, int stops,
                       LocalDate departDate, LocalDate returnDate, String bookingUrl) {
        this(airline, price, currency, stops, departDate, returnDate, null, null, List.of(), bookingUrl);
    }

    /**
     * Constructor WITHOUT the return departure time, for sources that can't provide
     * it (SerpApi) and for tests written before the field existed.
     */
    public FlightOffer(String airline, BigDecimal price, String currency, int stops,
                       LocalDate departDate, LocalDate returnDate, LocalTime departureTime,
                       List<String> stopovers, String bookingUrl) {
        this(airline, price, currency, stops, departDate, returnDate,
                departureTime, null, stopovers, bookingUrl);
    }

    /** Length of stay in days (exact difference between return and outbound). */
    public long durationDays() {
        return ChronoUnit.DAYS.between(departDate, returnDate);
    }
}
