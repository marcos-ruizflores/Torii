package com.torii.provider.amadeus;

import com.torii.config.AmadeusProperties;
import com.torii.provider.FlightProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Instant;

/**
 * Handles the Amadeus OAuth2 token.
 *
 * <p>Amadeus doesn't take the API key directly. You first request an access token
 * with the API key + secret, it expires after about 30 minutes, and that token goes
 * in an {@code Authorization: Bearer ...} header on every search.
 *
 * <p>This class requests the token once and <b>caches</b> it until shortly before it
 * expires, so we're not asking for a new one on every lookup. {@link #currentToken()}
 * is {@code synchronized} so two threads don't both request a token at once.
 */
public class AmadeusAuthClient {

    private static final Logger log = LoggerFactory.getLogger(AmadeusAuthClient.class);

    /** Safety margin: refresh the token 60s before it actually expires. */
    private static final long SAFETY_MARGIN_SECONDS = 60;

    private final RestClient restClient;
    private final AmadeusProperties props;
    private final Clock clock;

    private String cachedToken;
    private Instant expiresAt = Instant.EPOCH;

    public AmadeusAuthClient(RestClient.Builder builder, AmadeusProperties props, Clock clock) {
        this.restClient = builder.baseUrl(props.baseUrl()).build();
        this.props = props;
        this.clock = clock;
    }

    /** Returns a valid token, refreshing it if the current one has (almost) expired. */
    public synchronized String currentToken() {
        Instant now = clock.instant();
        boolean stillValid = cachedToken != null
                && now.isBefore(expiresAt.minusSeconds(SAFETY_MARGIN_SECONDS));
        if (stillValid) {
            return cachedToken;
        }
        return requestNewToken(now);
    }

    private String requestNewToken(Instant now) {
        // The token endpoint expects a form body (application/x-www-form-urlencoded).
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.apiKey());
        form.add("client_secret", props.apiSecret());

        try {
            AmadeusTokenResponse response = restClient.post()
                    .uri("/v1/security/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(AmadeusTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new FlightProviderException("Amadeus devolvió un token vacío");
            }

            cachedToken = response.accessToken();
            expiresAt = now.plusSeconds(response.expiresIn());
            log.info("Token de Amadeus renovado; válido durante {} s", response.expiresIn());
            return cachedToken;

        } catch (RestClientException e) {
            throw new FlightProviderException("No se pudo obtener el token de Amadeus: " + e.getMessage(), e);
        }
    }
}
