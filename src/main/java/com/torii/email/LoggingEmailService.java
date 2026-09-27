package com.torii.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No-op {@link EmailService}: sends nothing, just logs it. Used when email is disabled
 * or there's no API key, so the app and the tests work without credentials and
 * without sending real emails. Links are logged so the flows can be tried locally.
 */
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    private final String appUrl;

    public LoggingEmailService(String appUrl) {
        this.appUrl = appUrl;
    }

    @Override
    public void sendWelcome(String to, String name, String verificationToken) {
        log.info("[email deshabilitado] Bienvenida a {} ({}): {}", to, name,
                EmailLinks.verifyEmail(appUrl, verificationToken));
    }

    @Override
    public void sendPasswordReset(String to, String name, String token) {
        log.info("[email deshabilitado] Restablecer contraseña de {}: {}", to, EmailLinks.resetPassword(appUrl, token));
    }

    @Override
    public void sendEmailVerification(String to, String name, String token) {
        log.info("[email deshabilitado] Verificación de {}: {}", to, EmailLinks.verifyEmail(appUrl, token));
    }
}
