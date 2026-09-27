package com.torii.email;

/** A logged in user asked for a new email verification link. */
public record EmailVerificationRequestedEvent(String email, String name, String token) {
}
