package com.torii.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Transactional email settings (Resend), under the {@code torii.email} prefix.
 *
 * <p>Same as the other integrations: the {@code apiKey} NEVER goes in the code, it's
 * read from the {@code RESEND_API_KEY} env var in application.properties. It's
 * {@code enabled = false} by default, so the app starts (and tests run) with no key
 * and without sending anything. In that case a service that only logs what it would
 * send is used instead (see {@code EmailConfig}).
 *
 * <p>{@code fromAddress} defaults to Resend's test sender
 * ({@code onboarding@resend.dev}), which can only send to your own address. Change it
 * once there's a verified custom domain.
 */
@ConfigurationProperties(prefix = "torii.email")
public record EmailProperties(

        @DefaultValue("false") boolean enabled,
        @DefaultValue("https://api.resend.com") String baseUrl,
        @DefaultValue("") String apiKey,
        @DefaultValue("onboarding@resend.dev") String fromAddress,
        @DefaultValue("Torii") String fromName
) {

    /** Default values, handy for building the config in tests. */
    public static EmailProperties defaults() {
        return new EmailProperties(false, "https://api.resend.com", "", "onboarding@resend.dev", "Torii");
    }

    /** Copy with a different baseUrl (used in tests to point at the mock server). */
    public EmailProperties withBaseUrl(String newBaseUrl) {
        return new EmailProperties(enabled, newBaseUrl, apiKey, fromAddress, fromName);
    }

    /** Sender as "Name &lt;address&gt;", the format Resend expects. */
    public String from() {
        return fromName + " <" + fromAddress + ">";
    }
}
