package com.torii.email;

import com.torii.config.EmailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

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
        // The name comes straight from the sign up form: it's escaped before going into
        // the HTML, and control characters are dropped from the subject line.
        String displayName = name == null ? "" : name.replaceAll("\\p{Cntrl}", " ").strip();
        SendEmailRequest body = new SendEmailRequest(
                props.from(),
                List.of(to),
                "Bienvenido a Torii, " + displayName,
                welcomeHtml(HtmlUtils.htmlEscape(displayName)),
                welcomeText(displayName));

        restClient.post()
                .uri("/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("Email de bienvenida enviado a {}", to);
    }

    /**
     * Welcome email in the app's departures board look: graphite panel, TORII in flap
     * tiles and one signal yellow button. Email clients only reliably support tables
     * and inline styles, so that's all this uses. {@code safeName} must already be
     * HTML-escaped.
     */
    private String welcomeHtml(String safeName) {
        String url = props.appUrl();
        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta name="color-scheme" content="dark light">
                  <meta name="supported-color-schemes" content="dark light">
                  <title>Bienvenido a Torii</title>
                </head>
                <body style="margin:0;padding:0;background:#111317;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#111317;">
                    <tr><td align="center" style="padding:32px 16px;">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                             style="max-width:560px;background:#181b20;border:1px solid #22262d;border-radius:8px;">
                        <tr><td style="padding:28px 32px 8px;">
                          <table role="presentation" cellpadding="0" cellspacing="4" style="margin-left:-4px;">
                            <tr>%s</tr>
                          </table>
                        </td></tr>
                        <tr><td style="padding:16px 32px 0;font-family:'Barlow Semi Condensed',Arial,sans-serif;">
                          <h1 style="margin:0;font-size:26px;line-height:32px;font-weight:600;color:#eeeae0;">
                            Bienvenido a Torii, %s.
                          </h1>
                        </td></tr>
                        <tr><td style="padding:12px 32px 0;font-family:Barlow,Arial,sans-serif;font-size:16px;line-height:24px;color:#b2b7c0;">
                          Tu cuenta ya está lista. Tú eliges la ventana de vacaciones y cuántas noches
                          quieres estar; Torii prueba cada combinación de fechas y te enseña las más
                          baratas, con su veredicto de precio.
                        </td></tr>
                        <tr><td style="padding:20px 32px 0;">
                          <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                                 style="border-top:1px solid #22262d;border-bottom:1px solid #22262d;">
                            <tr>
                              <td style="padding:12px 0;font-family:'Barlow Semi Condensed',Arial,sans-serif;font-size:12px;letter-spacing:1px;text-transform:uppercase;color:#969ca6;">Tu plan</td>
                              <td align="right" style="padding:12px 0;font-family:'Barlow Semi Condensed',Arial,sans-serif;font-size:15px;font-weight:600;color:#eeeae0;">Gratis · 30 consultas al mes</td>
                            </tr>
                          </table>
                        </td></tr>
                        <tr><td style="padding:24px 32px 8px;">
                          <table role="presentation" cellpadding="0" cellspacing="0">
                            <tr><td style="background:#ffc629;border-radius:5px;">
                              <a href="%s" style="display:inline-block;padding:12px 22px;font-family:'Barlow Semi Condensed',Arial,sans-serif;font-size:16px;font-weight:600;color:#15120a;text-decoration:none;">Escanear fechas</a>
                            </td></tr>
                          </table>
                        </td></tr>
                        <tr><td style="padding:16px 32px 28px;font-family:Barlow,Arial,sans-serif;font-size:13px;line-height:20px;color:#707782;">
                          Recibes este correo porque acabas de crear una cuenta en
                          <a href="%s" style="color:#b2b7c0;">Torii</a>.
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(flapTiles("TORII"), safeName, url, url);
    }

    /** The wordmark as board tiles: one table cell per letter. */
    private static String flapTiles(String word) {
        StringBuilder cells = new StringBuilder();
        for (char c : word.toCharArray()) {
            cells.append("<td width=\"30\" height=\"38\" align=\"center\" valign=\"middle\" "
                    + "style=\"background:#2c313a;border-radius:4px;font-family:'Barlow Semi Condensed',Arial,sans-serif;"
                    + "font-size:22px;font-weight:600;color:#eeeae0;\">").append(c).append("</td>");
        }
        return cells.toString();
    }

    /** Plain text version: some clients show it, and spam filters like having one. */
    private String welcomeText(String name) {
        return """
                Bienvenido a Torii, %s.

                Tu cuenta ya está lista. Tú eliges la ventana de vacaciones y cuántas noches quieres estar; \
                Torii prueba cada combinación de fechas y te enseña las más baratas, con su veredicto de precio.

                Tu plan: Gratis, 30 consultas al mes.

                Escanear fechas: %s

                Recibes este correo porque acabas de crear una cuenta en Torii.
                """.formatted(name, props.appUrl());
    }

    /** JSON body the Resend API expects (POST /emails). */
    private record SendEmailRequest(String from, List<String> to, String subject, String html, String text) {
    }
}
