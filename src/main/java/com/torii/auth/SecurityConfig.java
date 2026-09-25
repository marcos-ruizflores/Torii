package com.torii.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API security: no sessions or cookies, JWTs only.
 *
 * <p>Rules:
 * <ul>
 *   <li>{@code /api/auth/**} is public (sign up and login).</li>
 *   <li>{@code /api/search} and {@code /api/price-history} are public, Torii works
 *       without an account. If the request has a token, the controller uses it to
 *       link the search to the user.</li>
 *   <li>{@code /api/me/**} needs a valid token (profile, my searches...).</li>
 * </ul>
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
                        .anyRequest().permitAll())
                // Validates the Bearer token on every request and exposes the Jwt as principal.
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }

    /** Verifies tokens with the SAME symmetric key they're signed with. */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(JwtService.secretKey(jwtSecret)).build();
    }

    /** BCrypt: slow salted hash, the usual choice for passwords. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
