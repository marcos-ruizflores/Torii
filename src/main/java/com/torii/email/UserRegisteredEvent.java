package com.torii.email;

/**
 * Domain event published right after a user signs up. It decouples sign up (which
 * only publishes the fact) from the welcome email (which listens for it), so an email
 * failure never breaks or slows down registration.
 */
public record UserRegisteredEvent(String email, String name) {
}
