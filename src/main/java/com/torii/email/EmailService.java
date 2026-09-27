package com.torii.email;

/**
 * Email sending abstraction. Just like {@code FlightProvider} decouples the algorithm
 * from the data source, this decouples the rest of the app from the email provider:
 * Resend today, something else tomorrow, without touching the callers.
 */
public interface EmailService {

    /**
     * Sends the welcome email to a newly registered user.
     *
     * @param to   recipient address
     * @param name user's name (to personalize the greeting)
     */
    void sendWelcome(String to, String name);
}
