package com.torii.email;

/**
 * Email sending abstraction. Just like {@code FlightProvider} decouples the algorithm
 * from the data source, this decouples the rest of the app from the email provider:
 * Resend today, something else tomorrow, without touching the callers.
 *
 * <p>Tokens arrive raw; each implementation turns them into links to the app.
 */
public interface EmailService {

    /** Welcome email for a new user, with the link to confirm their address. */
    void sendWelcome(String to, String name, String verificationToken);

    /** Link to choose a new password. */
    void sendPasswordReset(String to, String name, String token);

    /** Fresh link to confirm the address, when the user asks for it again. */
    void sendEmailVerification(String to, String name, String token);
}
