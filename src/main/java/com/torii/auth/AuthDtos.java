package com.torii.auth;

import com.torii.user.UserAccount;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTOs del API de autenticación, agrupados para no dispersar 4 mini-ficheros. */
public final class AuthDtos {

    private AuthDtos() {}

    public record SignupRequest(
            @NotBlank(message = "el nombre es obligatorio") String name,
            @NotBlank @Email(message = "email no válido") String email,
            @NotBlank @Size(min = 8, message = "la contraseña debe tener al menos 8 caracteres")
            String password
    ) {}

    public record LoginRequest(
            @NotBlank @Email(message = "email no válido") String email,
            @NotBlank String password
    ) {}

    /** Perfil público del usuario: lo que el frontend puede saber de una cuenta. */
    public record UserDto(Long id, String name, String email, String plan) {

        public static UserDto from(UserAccount user) {
            return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getPlan());
        }
    }

    /** Respuesta de login/registro: el token y el perfil, para no pedir /api/me extra. */
    public record AuthResponse(String token, UserDto user) {}
}
