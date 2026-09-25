package com.torii.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the Google Flights provider through SerpApi, under the
 * {@code torii.serpapi} prefix.
 *
 * <p>SerpApi sits in the middle and exposes Google Flights results as an API (Google
 * doesn't have a public one). Auth is much simpler than Amadeus: a single
 * {@code apiKey} passed as a URL param, no OAuth or tokens.
 *
 * <p>Like Amadeus, the key comes from an env var ({@code SERPAPI_KEY}) and the
 * integration is disabled by default.
 */
@ConfigurationProperties(prefix = "torii.serpapi")
public record SerpApiProperties(

        @DefaultValue("false") boolean enabled,
        @DefaultValue("https://serpapi.com") String baseUrl,
        @DefaultValue("") String apiKey,
        @DefaultValue("EUR") String currency,
        @DefaultValue("5") int maxResults
) {

    public static SerpApiProperties defaults() {
        return new SerpApiProperties(false, "https://serpapi.com", "", "EUR", 5);
    }

    public SerpApiProperties withBaseUrl(String newBaseUrl) {
        return new SerpApiProperties(enabled, newBaseUrl, apiKey, currency, maxResults);
    }
}
