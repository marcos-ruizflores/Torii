package com.torii.api;

import com.torii.model.SearchPrecision;
import com.torii.model.SearchRequest;
import com.torii.model.WeekPattern;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * API input object (the JSON body of the POST).
 *
 * <p>Format validation lives here and only here. The domain ({@link SearchRequest})
 * assumes whatever it receives is already valid. Keeping the DTO separate from the
 * domain means the API shape (fields, names) can change without dragging the domain
 * along.
 *
 * <p>Cross-field checks that an annotation can't express (e.g. "rangeEnd after
 * rangeStart" or "at least one stay fits in the range") are done in {@link #toDomain()}.
 */
public record SearchRequestDto(

        @NotBlank
        @Pattern(regexp = "[A-Za-z]{3}", message = "El origen debe ser un código IATA de 3 letras (ej. BCN)")
        String origin,

        @NotBlank
        @Pattern(regexp = "[A-Za-z]{3}", message = "El destino debe ser un código IATA de 3 letras (ej. NRT)")
        String destination,

        @NotNull
        @Future(message = "La fecha de inicio del rango debe ser futura")
        LocalDate rangeStart,

        @NotNull
        @Future(message = "La fecha de fin del rango debe ser futura")
        LocalDate rangeEnd,

        @Min(value = 1, message = "La duración base debe ser de al menos 1 día")
        @Max(value = 365, message = "La duración base no puede superar los 365 días")
        int baseDuration,

        @Min(value = 0, message = "La variabilidad no puede ser negativa")
        @Max(value = 30, message = "La variabilidad no puede superar los 30 días")
        int variability,

        @Min(value = 0, message = "Las escalas no pueden ser negativas")
        @Max(value = 3, message = "Como máximo 3 escalas")
        int maxStops,

        @Min(value = 1, message = "topN debe ser al menos 1")
        @Max(value = 50, message = "topN no puede superar 50")
        int topN,

        // Optional, defaults to EXHAUSTIVE (full coverage).
        // Allowed values: FAST, BALANCED, EXHAUSTIVE.
        String precision,

        // Optional budget: drops offers above this price. null/missing means no limit.
        // Applied as a filter, so it doesn't change the number of calls.
        @Positive(message = "maxPrice debe ser un número positivo")
        BigDecimal maxPrice,

        // Optional, but they go TOGETHER: weekend getaway filter. With
        // departDayOfWeek=FRIDAY and returnDayOfWeek=SUNDAY only the weekends in the
        // range are explored, and baseDuration/variability are ignored.
        // Allowed values: MONDAY..SUNDAY.
        String departDayOfWeek,
        String returnDayOfWeek
) {
    /**
     * Maps the DTO to the domain object, running the cross-field checks the
     * annotations can't cover.
     *
     * @throws IllegalArgumentException if the range doesn't make sense
     */
    public SearchRequest toDomain() {
        if (!rangeEnd.isAfter(rangeStart)) {
            throw new IllegalArgumentException("rangeEnd debe ser posterior a rangeStart");
        }
        WeekPattern weekPattern = parseWeekPattern();

        if (weekPattern != null) {
            // With a weekly pattern the pattern itself sets the length, so what we need
            // to check is that at least one full getaway fits in the range.
            LocalDate firstDeparture = weekPattern.firstDeparture(rangeStart);
            if (firstDeparture.plusDays(weekPattern.stayDays()).isAfter(rangeEnd)) {
                throw new IllegalArgumentException(
                        "En ese rango de fechas no cabe ninguna escapada de "
                                + dayLabel(weekPattern.departDay()) + " a "
                                + dayLabel(weekPattern.returnDay()));
            }
        } else {
            long rangeDays = ChronoUnit.DAYS.between(rangeStart, rangeEnd);
            // The longest stay we explore has to fit inside the range.
            if (baseDuration + variability >= rangeDays) {
                throw new IllegalArgumentException(
                        "El rango de vacaciones es demasiado corto para una estancia de "
                                + (baseDuration + variability) + " días");
            }
        }

        return new SearchRequest(
                origin.toUpperCase(), destination.toUpperCase(),
                rangeStart, rangeEnd, baseDuration, variability, maxStops, topN,
                parsePrecision(), maxPrice, weekPattern);
    }

    /**
     * Parses {@code precision} into the enum with a clear message when it's invalid.
     * Empty or null gives {@code null}: the search then runs at the finest precision
     * the user's plan includes (decided in {@code SearchService}).
     */
    private SearchPrecision parsePrecision() {
        if (precision == null || precision.isBlank()) {
            return null;
        }
        try {
            return SearchPrecision.valueOf(precision.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "precision no válida: '" + precision + "'. Valores admitidos: FAST, BALANCED, EXHAUSTIVE");
        }
    }

    /**
     * Builds the getaway filter, or {@code null} if it wasn't requested. Both days go
     * together: asking for just one is an error, since the trip length would be
     * unknown.
     */
    private WeekPattern parseWeekPattern() {
        boolean hasDepart = departDayOfWeek != null && !departDayOfWeek.isBlank();
        boolean hasReturn = returnDayOfWeek != null && !returnDayOfWeek.isBlank();

        if (!hasDepart && !hasReturn) {
            return null;
        }
        if (hasDepart != hasReturn) {
            throw new IllegalArgumentException(
                    "departDayOfWeek y returnDayOfWeek deben enviarse juntos");
        }
        return new WeekPattern(parseDay(departDayOfWeek, "departDayOfWeek"),
                parseDay(returnDayOfWeek, "returnDayOfWeek"));
    }

    private static DayOfWeek parseDay(String value, String field) {
        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    field + " no válido: '" + value + "'. Valores admitidos: MONDAY..SUNDAY");
        }
    }

    /** Day name in Spanish, only for error messages. */
    private static String dayLabel(DayOfWeek day) {
        return day.getDisplayName(TextStyle.FULL, Locale.of("es", "ES"));
    }
}
