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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.web.server.ResponseStatusException;
import com.torii.email.PasswordResetRequestedEvent;
import com.torii.email.UserRegisteredEvent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sign up and login tests against in-memory H2. Checks the password is stored
 * hashed, emails can't be duplicated and login only works with the right credentials.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AuthService.class, AuthTokenService.class, JwtService.class})
@RecordApplicationEvents
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

    @Autowired
    private AuthTokenRepository tokenRepository;

    @Autowired
    private ApplicationEvents events;

    private static final SignupRequest SIGNUP =
            new SignupRequest("Marcos", "marcos@test.com", "superclave123");

    @Test
    void elRegistroGuardaElUsuarioConLaPasswordHasheada() {
        AuthResponse response = authService.signup(SIGNUP);

        assertThat(response.token()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("marcos@test.com");
        assertThat(response.user().plan()).isEqualTo("FREE");

        UserAccount saved = users.findByEmail("marcos@test.com").orElseThrow();
        // Password must NEVER be stored in plain text, BCrypt hashes start with $2...
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

    private static void assertBadRequest(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void elEnlaceDelCorreoDeBienvenidaVerificaElEmailUnaSolaVez() {
        AuthResponse signup = authService.signup(SIGNUP);
        assertThat(signup.user().emailVerified()).isFalse();

        String token = events.stream(UserRegisteredEvent.class).findFirst().orElseThrow().verificationToken();
        assertThat(authService.verifyEmail(token).emailVerified()).isTrue();

        // Single use: the same link doesn't work twice.
        assertBadRequest(() -> authService.verifyEmail(token));
    }

    @Test
    void restablecerCambiaLaPasswordYElEnlaceNoSirveDosVeces() {
        authService.signup(SIGNUP);
        authService.requestPasswordReset(" Marcos@Test.com ");
        String token = events.stream(PasswordResetRequestedEvent.class).findFirst().orElseThrow().token();

        AuthResponse reset = authService.resetPassword(token, "clavenueva456");

        assertThat(reset.token()).isNotBlank();
        assertThat(reset.user().emailVerified()).isTrue(); // getting the email proves the address
        assertThat(authService.login(new LoginRequest("marcos@test.com", "clavenueva456")).token()).isNotBlank();
        assertThatThrownBy(() -> authService.login(new LoginRequest("marcos@test.com", "superclave123")))
                .isInstanceOf(ResponseStatusException.class);
        assertBadRequest(() -> authService.resetPassword(token, "otraclave789"));
    }

    @Test
    void pedirRestablecerConUnEmailDesconocidoNoFallaNiEnviaNada() {
        authService.requestPasswordReset("nadie@test.com");

        assertThat(events.stream(PasswordResetRequestedEvent.class)).isEmpty();
    }

    @Test
    void noSeEnviaMasDeUnEnlaceDeRestablecerPorMinuto() {
        authService.signup(SIGNUP);
        authService.requestPasswordReset("marcos@test.com");
        authService.requestPasswordReset("marcos@test.com");

        assertThat(events.stream(PasswordResetRequestedEvent.class)).hasSize(1);
    }

    @Test
    void unEnlaceCaducadoNoSirveYElNuevoAnulaAlAnterior() {
        UserAccount user = users.findByEmail(authService.signup(SIGNUP).user().email()).orElseThrow();
        // Far from the real clock, so the link sign up just sent doesn't trip the one-a-minute limit.
        Instant t0 = Instant.parse("2030-01-01T10:00:00Z");
        AuthTokenService at = new AuthTokenService(tokenRepository, Clock.fixed(t0, ZoneOffset.UTC));
        String first = at.issue(user, AuthTokenPurpose.PASSWORD_RESET).orElseThrow();

        // 31 minutes later the 30 minute link has expired...
        AuthTokenService later = new AuthTokenService(tokenRepository,
                Clock.fixed(t0.plus(Duration.ofMinutes(31)), ZoneOffset.UTC));
        assertBadRequest(() -> later.redeem(first, AuthTokenPurpose.PASSWORD_RESET));

        // ...a new one works, and asking for it burned the old one for good.
        String second = later.issue(user, AuthTokenPurpose.PASSWORD_RESET).orElseThrow();
        assertThat(later.redeem(second, AuthTokenPurpose.PASSWORD_RESET).getId()).isEqualTo(user.getId());
        // A token for one purpose is useless for the other.
        String verify = later.issue(user, AuthTokenPurpose.EMAIL_VERIFICATION).orElseThrow();
        assertBadRequest(() -> later.redeem(verify, AuthTokenPurpose.PASSWORD_RESET));
    }
}
