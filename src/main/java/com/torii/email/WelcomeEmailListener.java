package com.torii.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the welcome email when a {@link UserRegisteredEvent} arrives.
 *
 * <p>Two key decisions:
 * <ul>
 *   <li>{@code @TransactionalEventListener(AFTER_COMMIT)}: it only sends if the new
 *       user was actually committed to the DB. If the transaction rolls back, no email
 *       goes out.</li>
 *   <li>{@code @Async}: sending (a network call) runs in the background so sign up
 *       answers right away. The try/catch makes sure an email failure never
 *       propagates: at most it's a warning in the log.</li>
 * </ul>
 */
@Component
public class WelcomeEmailListener {

    private static final Logger log = LoggerFactory.getLogger(WelcomeEmailListener.class);

    private final EmailService emailService;

    public WelcomeEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            emailService.sendWelcome(event.email(), event.name());
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el email de bienvenida a {}: {}", event.email(), e.getMessage());
        }
    }
}
