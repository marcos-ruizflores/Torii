package com.torii.history;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A saved search as the frontend's "recent searches" list uses it. Has everything
 * needed to RUN the search again with one click (same fields as the form).
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
