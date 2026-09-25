package com.torii.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.torii.user.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Issues JWTs signed with a symmetric secret (HS256).
 *
 * <p>Login issues the token and from then on the frontend sends it with every request
 * ({@code Authorization: Bearer ...}). The backend keeps NO sessions, checking the
 * signature is enough. The token subject is the user id.
 *
 * <p>The secret comes from {@code TORII_JWT_SECRET}. The default value is ONLY for
 * local dev: with it anyone could forge tokens in production.
 */
@Service
public class JwtService {

    static final String DEV_SECRET = "torii-dev-secret-cambiame-en-produccion-0123456789";

    private final NimbusJwtEncoder encoder;
    private final Duration ttl;

    public JwtService(
            @Value("${torii.auth.jwt-secret:" + DEV_SECRET + "}") String secret,
            @Value("${torii.auth.token-ttl:24h}") Duration ttl) {
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(secret)));
        this.ttl = ttl;
    }

    /** Same key the SecurityConfig decoder uses, signing and verifying have to match. */
    static SecretKey secretKey(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String issueToken(UserAccount user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .build();
        return encoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
