package com.torii.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Traduce los errores de validación a respuestas HTTP 400 legibles, en vez de
 * dejar que Spring devuelva un volcado interno.
 *
 * <p>Usa {@link ProblemDetail} (RFC 7807), el formato estándar de errores de la API
 * web de Spring.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Errores de las anotaciones de Bean Validation (@NotBlank, @Min, etc.). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    /** Errores de las validaciones cruzadas de SearchRequestDto.toDomain(). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail onIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
