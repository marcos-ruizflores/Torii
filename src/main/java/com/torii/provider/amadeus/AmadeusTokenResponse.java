package com.torii.provider.amadeus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Respuesta del endpoint de token de Amadeus
 * ({@code POST /v1/security/oauth2/token}).
 *
 * <p>Solo modelamos los campos que usamos; {@code @JsonIgnoreProperties} descarta el
 * resto. Los nombres del JSON usan snake_case, así que los mapeamos con
 * {@code @JsonProperty}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AmadeusTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn
) {}
