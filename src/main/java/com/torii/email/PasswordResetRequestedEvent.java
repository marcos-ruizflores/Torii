package com.torii.email;

/** Someone asked for a password reset link for an existing account. */
public record PasswordResetRequestedEvent(String email, String name, String token) {
}
