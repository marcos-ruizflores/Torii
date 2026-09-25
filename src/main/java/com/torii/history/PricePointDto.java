package com.torii.history;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A history point in the shape the frontend chart expects (src/api/priceHistory.ts).
 * The field is {@code date} rather than {@code day} because that's the contract the
 * frontend already uses.
 */
public record PricePointDto(LocalDate date, BigDecimal price, String currency) {

    static PricePointDto from(PriceHistoryEntry entry) {
        return new PricePointDto(entry.getDay(), entry.getBestPrice(), entry.getCurrency());
    }
}
