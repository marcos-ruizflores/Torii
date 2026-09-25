package com.torii.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Amadeus integration settings, under the {@code torii.amadeus} prefix.
 *
 * <p>Credentials ({@code apiKey}, {@code apiSecret}) are NEVER hardcoded or committed:
 * {@code application.properties} reads them from environment variables
 * ({@code AMADEUS_API_KEY}, {@code AMADEUS_API_SECRET}). The integration is
 * {@code enabled = false} by default, so the app starts without any keys and nothing
 * changes until you turn it on.
 *
 * <p>{@code baseUrl} points to the free Amadeus test environment. Going to production
 * only means changing that URL.
 */
@ConfigurationProperties(prefix = "torii.amadeus")
public record AmadeusProperties(

        @DefaultValue("false") boolean enabled,
        @DefaultValue("https://test.api.amadeus.com") String baseUrl,
        @DefaultValue("") String apiKey,
        @DefaultValue("") String apiSecret,
        @DefaultValue("EUR") String currency,
        @DefaultValue("5") int maxResults
) {

    /** Default values, handy for building the config in tests. */
    public static AmadeusProperties defaults() {
        return new AmadeusProperties(false, "https://test.api.amadeus.com", "", "", "EUR", 5);
    }

    /** Copy with a different baseUrl (used in tests to point at the mock server). */
    public AmadeusProperties withBaseUrl(String newBaseUrl) {
        return new AmadeusProperties(enabled, newBaseUrl, apiKey, apiSecret, currency, maxResults);
    }
}
