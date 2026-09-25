package com.torii.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Turns validation errors into readable HTTP 400 responses instead of letting Spring
 * return its internal dump.
 *
 * <p>Uses {@link ProblemDetail} (RFC 7807), Spring's standard error format.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Bean Validation annotation errors (@NotBlank, @Min, etc.). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    /** Cross-field validation errors from SearchRequestDto.toDomain(). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail onIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Business errors thrown as ResponseStatusException (duplicate email 409, bad
     * credentials 401, quota exceeded 429...). Without this Spring returns its generic
     * error WITHOUT the message, and the frontend has nothing to show the user.
     */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ProblemDetail onResponseStatus(org.springframework.web.server.ResponseStatusException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
    }
}
