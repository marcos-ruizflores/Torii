package com.torii.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * FlightAPI.io provider settings, under the {@code torii.flightapi} prefix.
 *
 * <p>FlightAPI.io aggregates prices from lots of airlines and agencies
 * (Skyscanner-style data). Auth is a single {@code apiKey}, but unlike SerpApi it
 * goes in the URL <b>path</b>, not as a query param:
 * {@code /roundtrip/<api-key>/<origin>/<destination>/...}.
 *
 * <p>Same as the other providers, the key comes from an env var
 * ({@code FLIGHTAPI_KEY}) and the integration is disabled by default.
 */
@ConfigurationProperties(prefix = "torii.flightapi")
public record FlightApiProperties(

        @DefaultValue("false") boolean enabled,
        @DefaultValue("https://api.flightapi.io") String baseUrl,
        @DefaultValue("") String apiKey,
        @DefaultValue("EUR") String currency,
        @DefaultValue("Economy") String cabinClass,
        @DefaultValue("5") int maxResults
) {

    public static FlightApiProperties defaults() {
        return new FlightApiProperties(false, "https://api.flightapi.io", "", "EUR", "Economy", 5);
    }

    public FlightApiProperties withBaseUrl(String newBaseUrl) {
        return new FlightApiProperties(enabled, newBaseUrl, apiKey, currency, cabinClass, maxResults);
    }

    public FlightApiProperties withApiKey(String newApiKey) {
        return new FlightApiProperties(enabled, baseUrl, newApiKey, currency, cabinClass, maxResults);
    }
}
