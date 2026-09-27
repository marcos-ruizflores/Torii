package com.torii.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the account emails (welcome, password reset, email verification) when their
 * events arrive.
 *
 * <p>Two key decisions:
 * <ul>
 *   <li>{@code @TransactionalEventListener(AFTER_COMMIT)}: it only sends once the
 *       change (new user, new token) was actually committed. If the transaction rolls
 *       back, no email goes out with a link that doesn't work.</li>
 *   <li>{@code @Async}: sending (a network call) runs in the background so the request
 *       answers right away. For "forgot password" that also means the response takes
 *       the same time whether the email exists or not. The try/catch makes sure an
 *       email failure never propagates: at most it's a warning in the log.</li>
 * </ul>
 */
@Component
public class AccountEmailListener {

    private static final Logger log = LoggerFactory.getLogger(AccountEmailListener.class);

    private final EmailService emailService;

    public AccountEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        send("bienvenida", event.email(),
                () -> emailService.sendWelcome(event.email(), event.name(), event.verificationToken()));
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        send("restablecer contraseña", event.email(),
                () -> emailService.sendPasswordReset(event.email(), event.name(), event.token()));
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        send("verificación", event.email(),
                () -> emailService.sendEmailVerification(event.email(), event.name(), event.token()));
    }

    private void send(String kind, String to, Runnable sending) {
        try {
            sending.run();
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el email de {} a {}: {}", kind, to, e.getMessage());
        }
    }
}
