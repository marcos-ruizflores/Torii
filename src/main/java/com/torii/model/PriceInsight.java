package com.torii.model;

import java.math.BigDecimal;

/**
 * Verdict on a price: is it low, typical or high for this route?
 *
 * <p>{@code level} is {@code "low"}, {@code "typical"} or {@code "high"}, and
 * {@code typicalLow} / {@code typicalHigh} are the bounds of the usual price range
 * (either can be {@code null} if unknown). {@code source} says where the verdict
 * comes from, so the UI can say it:
 * <ul>
 *   <li>{@code "google"}: sent by the data source (Google's price insight).</li>
 *   <li>{@code "history"}: Torii's own, against the best daily prices it has stored
 *       for the route. {@code samples} is the number of days.</li>
 *   <li>{@code "scan"}: Torii's own, against the other dates scanned in this same
 *       search, when there isn't enough history yet. {@code samples} is the number
 *       of dates.</li>
 * </ul>
 */
public record PriceInsight(String level, BigDecimal typicalLow, BigDecimal typicalHigh,
                           String source, Integer samples) {

    public static final String GOOGLE = "google";
    public static final String HISTORY = "history";
    public static final String SCAN = "scan";

    /** Verdict sent by a data source. */
    public PriceInsight(String level, BigDecimal typicalLow, BigDecimal typicalHigh) {
        this(level, typicalLow, typicalHigh, GOOGLE, null);
    }
}
