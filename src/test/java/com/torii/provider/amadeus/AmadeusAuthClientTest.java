package com.torii.provider.amadeus;

import com.torii.config.AmadeusProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Amadeus token client test against a mock HTTP server ({@link MockRestServiceServer}).
 * No network or real keys: checks it requests the token once and then reuses the
 * cached one.
 */
class AmadeusAuthClientTest {

    @Test
    void pideElTokenUnaVezYLoCachea() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        AmadeusAuthClient auth = new AmadeusAuthClient(builder, AmadeusProperties.defaults(), clock);

        String tokenJson = """
                { "access_token": "ABC123", "token_type": "Bearer", "expires_in": 1799 }
                """;
        // Expect exactly ONE call to the token endpoint.
        server.expect(once(), requestTo(endsWith("/v1/security/oauth2/token")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenJson, APPLICATION_JSON));

        String first = auth.currentToken();
        String second = auth.currentToken(); // should come from the cache, no new call

        assertThat(first).isEqualTo("ABC123");
        assertThat(second).isEqualTo("ABC123");
        server.verify(); // fails if there was more than one HTTP call
    }
}
