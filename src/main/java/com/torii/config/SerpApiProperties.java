package com.torii.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración del proveedor "Google Flights" a través de SerpApi, bajo el prefijo
 * {@code torii.serpapi}.
 *
 * <p>SerpApi es un intermediario que expone los resultados de Google Flights como API
 * (Google no ofrece una API pública propia). La autenticación es mucho más simple que
 * la de Amadeus: una única {@code apiKey} que viaja como parámetro de la URL, sin
 * OAuth ni tokens.
 *
 * <p>Como con Amadeus, la clave se lee de una variable de entorno
 * ({@code SERPAPI_KEY}) y la integración está deshabilitada por defecto.
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
