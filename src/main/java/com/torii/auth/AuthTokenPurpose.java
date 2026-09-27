package com.torii.auth;

import java.time.Duration;

/** What an emailed link is for, and how long it stays valid. */
public enum AuthTokenPurpose {

    /** Short on purpose: whoever has the link can change the password. */
    PASSWORD_RESET(Duration.ofMinutes(30)),
    EMAIL_VERIFICATION(Duration.ofDays(2));

    private final Duration ttl;

    AuthTokenPurpose(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration ttl() {
        return ttl;
    }
}
