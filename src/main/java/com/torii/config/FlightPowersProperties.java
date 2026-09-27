package com.torii.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * FlightPowers settings (Google Flights data through RapidAPI), under the
 * {@code torii.flightpowers} prefix.
 *
 * <p>Auth is the RapidAPI key in the {@code x-rapidapi-key} header, plus the
 * {@code x-rapidapi-host} header RapidAPI uses to route the call. Like the other
 * providers, the key comes from an env var ({@code FLIGHTPOWERS_API_KEY}) and never
 * goes in the repo. Even when enabled, the provider is skipped if there's no key,
 * so deploying without it changes nothing.
 */
@ConfigurationProperties(prefix = "torii.flightpowers")
public record FlightPowersProperties(

        @DefaultValue("false") boolean enabled,
        @DefaultValue("https://google-flights-live-api.p.rapidapi.com") String baseUrl,
        @DefaultValue("google-flights-live-api.p.rapidapi.com") String rapidApiHost,
        @DefaultValue("") String apiKey,
        @DefaultValue("EUR") String currency,
        @DefaultValue("5") int maxResults
) {

    public static FlightPowersProperties defaults() {
        return new FlightPowersProperties(false, "https://google-flights-live-api.p.rapidapi.com",
                "google-flights-live-api.p.rapidapi.com", "", "EUR", 5);
    }

    public FlightPowersProperties withBaseUrl(String newBaseUrl) {
        return new FlightPowersProperties(enabled, newBaseUrl, rapidApiHost, apiKey, currency, maxResults);
    }

    public FlightPowersProperties withApiKey(String newApiKey) {
        return new FlightPowersProperties(enabled, baseUrl, rapidApiHost, newApiKey, currency, maxResults);
    }
}
