package com.torii.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de la integración con Amadeus, bajo el prefijo {@code torii.amadeus}.
 *
 * <p>Las credenciales ({@code apiKey}, {@code apiSecret}) NUNCA se escriben en el
 * código ni en el repositorio: en {@code application.properties} se leen de variables
 * de entorno ({@code AMADEUS_API_KEY}, {@code AMADEUS_API_SECRET}). Por defecto la
 * integración está {@code enabled = false}, de modo que la aplicación arranca sin
 * claves y nada cambia hasta que tú la actives.
 *
 * <p>{@code baseUrl} apunta al entorno de pruebas gratuito de Amadeus; el día que
 * pases a producción, solo cambia esa URL.
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

    /** Valores por defecto, útiles para construir la configuración en tests. */
    public static AmadeusProperties defaults() {
        return new AmadeusProperties(false, "https://test.api.amadeus.com", "", "", "EUR", 5);
    }

    /** Copia con otra baseUrl (cómodo en tests para apuntar al servidor simulado). */
    public AmadeusProperties withBaseUrl(String newBaseUrl) {
        return new AmadeusProperties(enabled, newBaseUrl, apiKey, apiSecret, currency, maxResults);
    }
}
