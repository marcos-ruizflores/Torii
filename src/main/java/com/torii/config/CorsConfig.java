package com.torii.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS config so the frontend, deployed on a DIFFERENT origin (domain/port), can call
 * this API from the browser.
 *
 * <p>Not needed in dev because the Vite proxy makes the browser think it's talking to
 * its own origin. But once frontend and backend are deployed separately (e.g. the SPA
 * on Vercel and the API somewhere else), the browser enforces the same-origin policy
 * and blocks the calls unless the backend explicitly lists which origins it accepts.
 * That's what this does.
 *
 * <p>It's exposed as the {@code corsConfigurationSource} bean because that's what
 * Spring Security looks for in its CORS filter, which runs BEFORE the rest of the
 * security chain. Without that, OPTIONS preflights end up as 401s.
 *
 * <p>Origins are set with {@code torii.cors.allowed-origins} (comma separated). Never
 * use "*" in production.
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    private final List<String> allowedOrigins;

    public CorsConfig(
            @Value("${torii.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
        log.info("CORS: orígenes permitidos para /api/**: {}", allowedOrigins);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        // The frontend sends Content-Type and Authorization (the JWT).
        config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        // Cache the preflight response for 1h so the browser doesn't send an OPTIONS
        // before every request.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
