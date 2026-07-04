package com.torii.history;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Una búsqueda guardada, tal y como la consume el frontend en "Mis últimas
 * búsquedas". Contiene todo lo necesario para poder REPETIR la búsqueda con un
 * clic (los mismos campos que el formulario).
 */
public record SavedSearchDto(
        Long id,
        String origin,
        String destination,
        LocalDate rangeStart,
        LocalDate rangeEnd,
        int baseDuration,
        int variability,
        int maxStops,
        int topN,
        String precision,
        BigDecimal maxPrice,
        Instant createdAt
) {
    static SavedSearchDto from(SearchRecord record) {
        return new SavedSearchDto(record.getId(), record.getOrigin(), record.getDestination(),
                record.getRangeStart(), record.getRangeEnd(), record.getBaseDuration(),
                record.getVariability(), record.getMaxStops(), record.getTopN(),
                record.getPrecision(), record.getMaxPrice(), record.getCreatedAt());
    }
}
