package com.torii.email;

import com.torii.config.EmailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;

import java.util.concurrent.Executor;

/**
 * Wires up email sending.
 *
 * <p>Picks the {@link EmailService} implementation from the config: if email is
 * enabled AND there's an API key it uses Resend for real, otherwise it uses
 * {@link LoggingEmailService} (sends nothing, just logs) so everything works locally
 * and in tests without credentials.
 *
 * <p>{@code @EnableAsync} lets the email listener run in the background (see
 * {@code WelcomeEmailListener}) on a dedicated virtual thread executor.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

    @Bean
    public EmailService emailService(EmailProperties props) {
        if (props.enabled() && !props.apiKey().isBlank()) {
            log.info("Email activo: se enviarán correos vía Resend desde {}", props.from());
            return new ResendEmailService(RestClient.builder(), props);
        }
        log.info("Email deshabilitado (torii.email.enabled=false o sin RESEND_API_KEY): "
                + "los correos solo se registrarán en el log.");
        return new LoggingEmailService(props.appUrl());
    }

    /**
     * Dedicated executor for emails: virtual threads, one per task. Sending (a network
     * call) doesn't block the request thread or compete with the rest of the app.
     * Referenced by name in {@code @Async("emailTaskExecutor")}.
     */
    @Bean("emailTaskExecutor")
    public Executor emailTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("email-");
        executor.setVirtualThreads(true);
        return executor;
    }
}
