package com.torii.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No-op {@link EmailService}: sends nothing, just logs it. Used when email is disabled
 * or there's no API key, so the app and the tests work without credentials and
 * without sending real emails.
 */
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void sendWelcome(String to, String name) {
        log.info("[email deshabilitado] Se enviaría bienvenida a {} ({})", to, name);
    }
}
