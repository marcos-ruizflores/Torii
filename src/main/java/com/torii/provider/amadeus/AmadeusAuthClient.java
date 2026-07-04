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
 * Gestiona el token OAuth2 de Amadeus.
 *
 * <p>Amadeus no usa una API key directa: primero hay que pedir un token de acceso
 * (con la API Key + Secret) que caduca a la media hora, y luego ese token se manda
 * como cabecera {@code Authorization: Bearer ...} en cada búsqueda.
 *
 * <p>Esta clase pide el token una vez y lo <b>cachea</b> hasta poco antes de que
 * caduque, renovándolo solo cuando hace falta. Así no pedimos un token nuevo en cada
 * búsqueda. El método {@link #currentToken()} es {@code synchronized} para que dos
 * hilos no pidan token a la vez.
 */
public class AmadeusAuthClient {

    private static final Logger log = LoggerFactory.getLogger(AmadeusAuthClient.class);

    /** Margen de seguridad: renovamos el token 60 s antes de su caducidad real. */
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

    /** Devuelve un token válido, renovándolo si el actual ha caducado (o casi). */
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
        // El endpoint de token espera un formulario (application/x-www-form-urlencoded).
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
