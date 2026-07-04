package com.torii.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
 * <p>Los orígenes se configuran con {@code torii.cors.allowed-origins} (lista
 * separada por comas), así cada entorno permite solo los suyos: en local el dev
 * server de Vite, en producción el dominio real del frontend. Nunca usar "*" en
 * producción.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    private final List<String> allowedOrigins;

    public CorsConfig(
            @Value("${torii.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
        log.info("CORS: orígenes permitidos para /api/**: {}", allowedOrigins);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST")
                // Cachear la respuesta de preflight 1h: el navegador no repite el
                // OPTIONS en cada petición.
                .maxAge(3600);
    }
}
