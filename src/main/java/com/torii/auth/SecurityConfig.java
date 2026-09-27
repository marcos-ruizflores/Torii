package com.torii.auth;

import com.torii.user.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API security: no sessions or cookies, JWTs only.
 *
 * <p>Rules:
 * <ul>
 *   <li>{@code /api/auth/**} is public (sign up and login).</li>
 *   <li>{@code POST /api/search} needs a token: searching uses quota and paid external
 *       API calls, so only registered users can do it. Anonymous users get a 401.</li>
 *   <li>{@code /api/price-history} is public: it's cheap read-only data from the DB
 *       and works as a showcase for people without an account yet.</li>
 *   <li>{@code /api/me/**} needs a valid token (profile, my searches...).</li>
 * </ul>
 *
 * <p>A token also stops being valid when its account is gone or its password changed
 * after the token was issued (see {@link #jwtDecoder}).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String jwtSecret;

    public SecurityConfig(@Value("${torii.auth.jwt-secret:" + JwtService.DEV_SECRET + "}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // No session cookies means no CSRF to protect against: the token goes
                // in the Authorization header and a third party can't force that.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults()) // picks up the CorsConfig bean
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/me/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/search").authenticated()
                        .anyRequest().permitAll())
                // Validates the Bearer token on every request and exposes the Jwt as principal.
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }

    /**
     * Verifies tokens with the SAME symmetric key they're signed with. On top of the
     * default checks (signature, expiry) it rejects tokens issued for an older
     * password: each token carries the password version it was issued for (the "pwc"
     * claim) and it has to match the account's current one. That's what makes a reset
     * log out other sessions. It costs one lookup by primary key per authenticated
     * request. Tokens from before the claim existed count as version 0.
     *
     * <p>The repository is optional so the {@code @WebMvcTest} slices, which have no
     * JPA, still get a decoder.
     */
    @Bean
    public JwtDecoder jwtDecoder(ObjectProvider<UserRepository> users) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(JwtService.secretKey(jwtSecret)).build();
        UserRepository repository = users.getIfAvailable();
        if (repository != null) {
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefault(), jwt -> checkPasswordChange(repository, jwt)));
        }
        return decoder;
    }

    private static OAuth2TokenValidatorResult checkPasswordChange(UserRepository users, Jwt jwt) {
        Object claim = jwt.getClaims().get(JwtService.PASSWORD_VERSION_CLAIM);
        long tokenVersion = claim instanceof Number n ? n.longValue() : 0;
        boolean valid = users.findById(Long.valueOf(jwt.getSubject()))
                .map(user -> user.passwordVersion() == tokenVersion)
                .orElse(false);
        return valid
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Sesión caducada", null));
    }

    /** BCrypt: slow salted hash, the usual choice for passwords. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
