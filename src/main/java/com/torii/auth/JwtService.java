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
 * Emisión de tokens JWT firmados con un secreto simétrico (HS256).
 *
 * <p>El token es la "entrada de cine" del usuario: el login lo emite y, a partir de
 * ahí, el frontend lo manda en cada petición ({@code Authorization: Bearer ...}).
 * El backend NO guarda sesiones: le basta verificar la firma. El subject del token
 * es el id del usuario.
 *
 * <p>El secreto viene de {@code TORII_JWT_SECRET}; el valor por defecto es SOLO
 * para desarrollo (con él, cualquiera podría falsificar tokens en producción).
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

    /** La misma clave que usa el decoder de SecurityConfig: firma y verificación deben coincidir. */
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
