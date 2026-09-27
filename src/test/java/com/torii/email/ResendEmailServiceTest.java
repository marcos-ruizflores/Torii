package com.torii.email;

import com.torii.config.EmailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Resend email test against a mock HTTP server (no network or real key). Checks that
 * it sends POST /emails with the API key in the header and the right body, and that
 * a Resend error surfaces as an exception.
 */
class ResendEmailServiceTest {

    private MockRestServiceServer server;
    private ResendEmailService service;

    // enabled/baseUrl don't matter here (MockRestServiceServer intercepts), but the
    // apiKey (goes in the header) and the sender (goes in the body) do.
    private static final EmailProperties PROPS =
            new EmailProperties(true, "https://api.resend.com", "re_test_key", "onboarding@resend.dev", "Torii",
                    "https://www.toriitravel.com");

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new ResendEmailService(builder, PROPS);
    }

    @Test
    void enviaPostAResendConLaApiKeyYElCuerpoCorrecto() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_test_key"))
                .andExpect(jsonPath("$.from").value("Torii <onboarding@resend.dev>"))
                .andExpect(jsonPath("$.to[0]").value("marcos@example.com"))
                .andExpect(jsonPath("$.subject", containsString("Marcos")))
                .andExpect(jsonPath("$.html", allOf(containsString("Marcos"),
                        containsString("href=\"https://www.toriitravel.com/verificar-email?token=tok_123\""))))
                .andExpect(jsonPath("$.text", containsString("https://www.toriitravel.com")))
                .andRespond(withSuccess("{\"id\":\"abc-123\"}", APPLICATION_JSON));

        service.sendWelcome("marcos@example.com", "Marcos", "tok_123");

        server.verify();
    }

    @Test
    void siResendDevuelveErrorLanzaExcepcion() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> service.sendWelcome("marcos@example.com", "Marcos", "tok_123"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void elNombreSeEscapaEnElHtmlYNoRompeElAsunto() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(jsonPath("$.html", allOf(
                        containsString("&lt;script&gt;"), not(containsString("<script>")))))
                .andExpect(jsonPath("$.subject", not(containsString("\n"))))
                .andRespond(withSuccess("{\"id\":\"abc-123\"}", APPLICATION_JSON));

        service.sendWelcome("marcos@example.com", "<script>alert(1)</script>\nBcc: x@y.z", "tok_123");

        server.verify();
    }

    @Test
    void elCorreoDeRestablecerLlevaElEnlaceConElToken() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(jsonPath("$.to[0]").value("marcos@example.com"))
                .andExpect(jsonPath("$.subject", containsString("contraseña")))
                .andExpect(jsonPath("$.html",
                        containsString("https://www.toriitravel.com/restablecer-contrasena?token=tok_456")))
                .andExpect(jsonPath("$.text",
                        containsString("https://www.toriitravel.com/restablecer-contrasena?token=tok_456")))
                .andRespond(withSuccess("{\"id\":\"abc-123\"}", APPLICATION_JSON));

        service.sendPasswordReset("marcos@example.com", "Marcos", "tok_456");

        server.verify();
    }
}
