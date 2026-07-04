package com.torii.auth;

import com.torii.auth.AuthDtos.AuthResponse;
import com.torii.auth.AuthDtos.LoginRequest;
import com.torii.auth.AuthDtos.SignupRequest;
import com.torii.user.UserAccount;
import com.torii.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de registro y login contra la H2 en memoria. Verifican que la contraseña
 * se guarda hasheada, que el email no se puede duplicar y que el login solo pasa
 * con las credenciales correctas.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AuthService.class, JwtService.class})
class AuthServiceTest {

    @TestConfiguration
    static class PasswordConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository users;

    private static final SignupRequest SIGNUP =
            new SignupRequest("Marcos", "marcos@test.com", "superclave123");

    @Test
    void elRegistroGuardaElUsuarioConLaPasswordHasheada() {
        AuthResponse response = authService.signup(SIGNUP);

        assertThat(response.token()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("marcos@test.com");
        assertThat(response.user().plan()).isEqualTo("FREE");

        UserAccount saved = users.findByEmail("marcos@test.com").orElseThrow();
        // La contraseña NUNCA en claro: BCrypt produce hashes con prefijo $2...
        assertThat(saved.getPasswordHash()).isNotEqualTo("superclave123").startsWith("$2");
    }

    @Test
    void noPermiteDosCuentasConElMismoEmail() {
        authService.signup(SIGNUP);

        assertThatThrownBy(() -> authService.signup(
                new SignupRequest("Otro", "MARCOS@test.com", "otraclave123")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void elLoginDevuelveTokenConCredencialesCorrectas() {
        authService.signup(SIGNUP);

        AuthResponse response = authService.login(new LoginRequest("marcos@test.com", "superclave123"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.user().name()).isEqualTo("Marcos");
    }

    @Test
    void elLoginRechazaPasswordIncorrecta() {
        authService.signup(SIGNUP);

        assertThatThrownBy(() -> authService.login(new LoginRequest("marcos@test.com", "malamala123")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
