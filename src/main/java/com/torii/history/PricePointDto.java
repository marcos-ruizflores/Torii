package com.torii.history;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Punto del histórico tal y como lo consume el gráfico del frontend
 * (src/api/priceHistory.ts). El campo se llama {@code date} (no {@code day})
 * porque ese es el contrato que el frontend ya espera.
 */
public record PricePointDto(LocalDate date, BigDecimal price, String currency) {

    static PricePointDto from(PriceHistoryEntry entry) {
        return new PricePointDto(entry.getDay(), entry.getBestPrice(), entry.getCurrency());
    }
}
