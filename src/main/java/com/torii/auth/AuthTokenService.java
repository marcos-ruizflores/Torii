package com.torii.auth;

import com.torii.user.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Issues and redeems the single-use tokens behind password reset and email
 * verification links.
 *
 * <p>Tokens are 32 random bytes (base64url, so they go in a URL as is). The DB only
 * keeps their SHA-256: a plain hash is enough here because the input is random, not
 * a password someone could guess. Issuing a new link burns the previous ones, and a
 * user can't ask for more than one link per minute (it would just be email spam).
 */
@Service
public class AuthTokenService {

    static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);

    private final AuthTokenRepository tokens;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public AuthTokenService(AuthTokenRepository tokens) {
        this(tokens, Clock.systemUTC());
    }

    /** For tests that need to move time forward. */
    AuthTokenService(AuthTokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    /**
     * New raw token for the user, or empty if they already got one in the last
     * minute. The raw value must go straight into the email, it's not stored.
     */
    @Transactional
    public Optional<String> issue(UserAccount user, AuthTokenPurpose purpose) {
        Instant now = clock.instant();
        boolean tooSoon = tokens.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose)
                .map(t -> t.getCreatedAt().isAfter(now.minus(RESEND_COOLDOWN)))
                .orElse(false);
        if (tooSoon) {
            return Optional.empty();
        }
        tokens.invalidatePending(user.getId(), purpose, now);

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new AuthToken(user, purpose, sha256(raw), now));
        return Optional.of(raw);
    }

    /** Marks the token as used and returns its owner. Unknown, used or expired: 400. */
    @Transactional
    public UserAccount redeem(String raw, AuthTokenPurpose purpose) {
        Instant now = clock.instant();
        AuthToken token = tokens.findByTokenHashAndPurpose(sha256(raw), purpose)
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El enlace no es válido o ha caducado. Pide uno nuevo."));
        token.markUsed(now);
        return token.getUser();
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
