package com.torii.auth;

import com.torii.auth.AuthDtos.AuthResponse;
import com.torii.auth.AuthDtos.LoginRequest;
import com.torii.auth.AuthDtos.SignupRequest;
import com.torii.auth.AuthDtos.UserDto;
import com.torii.user.UserAccount;
import com.torii.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * User sign up and login.
 *
 * <p>Sign up stores the account with a BCrypt password hash and returns a token, so
 * the user is logged in right away. Login checks the hash and issues a token. Errors
 * don't give away more than needed ("unknown email" and "wrong password" look the
 * same).
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = request.email().toLowerCase().strip();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese email");
        }
        UserAccount user = users.save(new UserAccount(
                email, passwordEncoder.encode(request.password()), request.name().strip()));
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
}
