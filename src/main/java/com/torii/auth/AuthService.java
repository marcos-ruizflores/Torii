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
 * Registro y login de usuarios.
 *
 * <p>Registro: guarda la cuenta con la contraseña hasheada (BCrypt) y devuelve un
 * token, de modo que registrarse deja al usuario ya logueado. Login: comprueba el
 * hash y emite token. En ambos errores se responde sin dar pistas de más (no se
 * distingue "email no existe" de "contraseña incorrecta").
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
