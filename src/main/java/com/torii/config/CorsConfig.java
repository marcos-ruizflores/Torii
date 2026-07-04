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
 * Configuración CORS: permite que el frontend, desplegado en OTRO origen (otro
 * dominio/puerto), pueda llamar a esta API desde el navegador.
 *
 * <p>Contexto: en desarrollo no hace falta, porque el proxy de Vite hace que el
 * navegador crea que habla con su propio origen. Pero con el frontend y el backend
 * desplegados por separado (p. ej. la SPA en Vercel y la API en otro servidor), el
 * navegador aplica la "same-origin policy" y bloquea las llamadas salvo que el
 * backend declare explícitamente qué orígenes acepta — que es esto.
 *
 * <p>Se expone como bean {@code corsConfigurationSource} porque es lo que Spring
 * Security busca para su filtro CORS (que corre ANTES que el resto de la cadena de
 * seguridad, imprescindible para que los preflight OPTIONS no acaben en 401).
 *
 * <p>Los orígenes se configuran con {@code torii.cors.allowed-origins} (lista
 * separada por comas). Nunca usar "*" en producción.
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
        // El frontend manda Content-Type y Authorization (el token JWT).
        config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        // Cachear la respuesta de preflight 1h: el navegador no repite el OPTIONS
        // en cada petición.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
