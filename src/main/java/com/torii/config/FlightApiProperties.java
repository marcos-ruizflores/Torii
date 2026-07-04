package com.torii.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración del proveedor FlightAPI.io, bajo el prefijo {@code torii.flightapi}.
 *
 * <p>FlightAPI.io agrega precios de múltiples aerolíneas y agencias (datos estilo
 * Skyscanner). La autenticación es una única {@code apiKey}, pero a diferencia de
 * SerpApi viaja como <b>segmento del path</b> de la URL, no como query param:
 * {@code /roundtrip/<api-key>/<origen>/<destino>/...}.
 *
 * <p>Como con el resto de proveedores, la clave se lee de una variable de entorno
 * ({@code FLIGHTAPI_KEY}) y la integración está deshabilitada por defecto.
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
