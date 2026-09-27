package com.torii.email;

import com.torii.config.EmailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * {@link EmailService} backed by the Resend API (https://resend.com).
 *
 * <p>Same pattern as the flight providers: it gets a {@link RestClient.Builder}
 * injected (so tests can swap in a {@code MockRestServiceServer} with no network) and
 * all the config comes from {@link EmailProperties}. The key goes in the
 * {@code Authorization} header.
 */
public class ResendEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private final RestClient restClient;
    private final EmailProperties props;

    public ResendEmailService(RestClient.Builder builder, EmailProperties props) {
        this.restClient = builder.baseUrl(props.baseUrl()).build();
        this.props = props;
    }

    @Override
    public void sendWelcome(String to, String name) {
        SendEmailRequest body = new SendEmailRequest(
                props.from(),
                List.of(to),
                "¡Bienvenido a Torii, " + name + "! 🐦",
                welcomeHtml(name));

        restClient.post()
                .uri("/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("Email de bienvenida enviado a {}", to);
    }

    private String welcomeHtml(String name) {
        return """
                <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto;">
                  <h1 style="color: #1570ef;">¡Gracias por registrarte, %s!</h1>
                  <p>Torii ya está listo para buscarte la oferta de vuelo más barata dentro
                     de tu rango de vacaciones.</p>
                  <p>Explora un rango de fechas, elige tu duración de estancia y deja que
                     Torii encuentre el mejor precio. ✈️</p>
                  <p style="color: #667085; font-size: 13px;">Un saludo,<br/>El equipo de Torii</p>
                </div>
                """.formatted(name);
    }

    /** JSON body the Resend API expects (POST /emails). */
    private record SendEmailRequest(String from, List<String> to, String subject, String html) {
    }
}
