package com.torii.auth;

import com.torii.auth.AuthDtos.AuthResponse;
import com.torii.auth.AuthDtos.LoginRequest;
import com.torii.auth.AuthDtos.SignupRequest;
import com.torii.auth.AuthDtos.UserDto;
import com.torii.email.EmailVerificationRequestedEvent;
import com.torii.email.PasswordResetRequestedEvent;
import com.torii.email.UserRegisteredEvent;
import com.torii.user.UserAccount;
import com.torii.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * User sign up and login.
 *
 * <p>Sign up stores the account with a BCrypt password hash and returns a token, so
 * the user is logged in right away. Login checks the hash and issues a token. Errors
 * don't give away more than needed ("unknown email" and "wrong password" look the
 * same).
 *
 * <p>It also owns the emailed-link flows: password reset and email verification. Both
 * go through {@link AuthTokenService} (single-use, hashed, short-lived tokens).
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher events;
    private final AuthTokenService tokens;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       ApplicationEventPublisher events, AuthTokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.events = events;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = request.email().toLowerCase().strip();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese email");
        }
        UserAccount user = users.save(new UserAccount(
                email, passwordEncoder.encode(request.password()), request.name().strip()));
        // The welcome email goes out from an AFTER_COMMIT listener: only if the sign up
        // actually commits, and without slowing down or breaking it if the email fails.
        String verification = tokens.issue(user, AuthTokenPurpose.EMAIL_VERIFICATION).orElseThrow();
        events.publishEvent(new UserRegisteredEvent(user.getEmail(), user.getName(), verification));
        return new AuthResponse(jwtService.issueToken(user), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount user = users.findByEmail(request.email().toLowerCase().strip())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos"));
        return new AuthResponse(jwtService.issueToken(user), UserDto.from(user));
    }

    /**
     * Sends a reset link if the email belongs to an account. The caller always gets
     * the same answer, so this can't be used to find out who has an account.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        users.findByEmail(email.toLowerCase().strip()).ifPresent(user ->
                tokens.issue(user, AuthTokenPurpose.PASSWORD_RESET).ifPresent(token ->
                        events.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), user.getName(), token))));
    }

    /**
     * Sets the new password and logs the user in. Tokens issued before now stop
     * working (see SecurityConfig), so other open sessions are closed. Getting the
     * link also proves the address, so the email counts as verified.
     */
    @Transactional
    public AuthResponse resetPassword(String token, String newPassword) {
        UserAccount user = tokens.redeem(token, AuthTokenPurpose.PASSWORD_RESET);
        user.changePassword(passwordEncoder.encode(newPassword), Instant.now());
        user.markEmailVerified();
        return new AuthResponse(jwtService.issueToken(user), UserDto.from(user));
    }

    @Transactional
    public UserDto verifyEmail(String token) {
        UserAccount user = tokens.redeem(token, AuthTokenPurpose.EMAIL_VERIFICATION);
        user.markEmailVerified();
        return UserDto.from(user);
    }

    /** New verification link for a logged in user. No-op if already verified or asked a minute ago. */
    @Transactional
    public void resendVerification(Long userId) {
        UserAccount user = users.findById(userId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "La cuenta del token ya no existe"));
        if (user.isEmailVerified()) {
            return;
        }
        tokens.issue(user, AuthTokenPurpose.EMAIL_VERIFICATION).ifPresent(token ->
                events.publishEvent(new EmailVerificationRequestedEvent(user.getEmail(), user.getName(), token)));
    }
}
