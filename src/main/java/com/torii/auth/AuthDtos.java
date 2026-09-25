package com.torii.auth;

import com.torii.user.UserAccount;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Auth API DTOs, grouped here instead of four tiny files. */
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

    /** Public user profile: what the frontend is allowed to know about an account. */
    public record UserDto(Long id, String name, String email, String plan) {

        public static UserDto from(UserAccount user) {
            return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getPlan());
        }
    }

    /** Login/sign up response: token plus profile, saves an extra call to /api/me. */
    public record AuthResponse(String token, UserDto user) {}
}
