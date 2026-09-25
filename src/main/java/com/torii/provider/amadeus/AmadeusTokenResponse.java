package com.torii.provider.amadeus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from the Amadeus token endpoint ({@code POST /v1/security/oauth2/token}).
 *
 * <p>Only the fields we use are mapped, {@code @JsonIgnoreProperties} drops the rest.
 * The JSON uses snake_case, hence the {@code @JsonProperty} annotations.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AmadeusTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn
) {}
