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
 * Seguridad de la API: sin sesiones ni cookies, solo tokens JWT.
 *
 * <p>Reglas:
 * <ul>
 *   <li>{@code /api/auth/**} es público (registro y login).</li>
 *   <li>{@code /api/search} y {@code /api/price-history} son públicos: Torii se
 *       puede usar sin cuenta. Si la petición trae token, el controlador lo usa
 *       para asociar la búsqueda al usuario.</li>
 *   <li>{@code /api/me/**} requiere token válido (perfil, mis búsquedas...).</li>
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
                // Sin cookies de sesión no hay CSRF que proteger: el token viaja en
                // la cabecera Authorization y un tercero no puede forzarla.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults()) // usa el bean de CorsConfig
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/me/**").authenticated()
                        .anyRequest().permitAll())
                // Valida el Bearer token de cada petición y expone el Jwt como principal.
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }

    /** Verificación de tokens con la MISMA clave simétrica con la que se firman. */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(JwtService.secretKey(jwtSecret)).build();
    }

    /** BCrypt: hash lento y con sal, el estándar para contraseñas. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
