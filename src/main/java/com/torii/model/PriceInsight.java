package com.torii.model;

import java.math.BigDecimal;

/**
 * Google's verdict on a price compared to other dates for the same route.
 *
 * <p>{@code level} is {@code "low"}, {@code "typical"} or {@code "high"}, and
 * {@code typicalLow} / {@code typicalHigh} are the bounds of the usual price range
 * (either can be {@code null} if the source doesn't send it). Only some sources
 * provide this, so offers from the rest carry {@code null}.
 */
public record PriceInsight(String level, BigDecimal typicalLow, BigDecimal typicalHigh) {
}
