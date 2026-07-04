package com.torii.api;

import com.torii.model.SearchPrecision;
import com.torii.model.SearchRequest;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Objeto de entrada de la API (lo que llega en el JSON del POST).
 *
 * <p>Aquí —y solo aquí— viven las reglas de validación de formato. El dominio
 * ({@link SearchRequest}) confía en que cualquier cosa que reciba ya está validada.
 * Esta separación DTO ↔ dominio es una buena práctica que conviene coger desde el
 * principio: la API puede cambiar de forma (campos, nombres) sin arrastrar al
 * dominio.
 *
 * <p>Las validaciones cruzadas que una anotación no puede expresar (p. ej.
 * "rangeEnd posterior a rangeStart" o "que quepa al menos una estancia en el rango")
 * se comprueban en {@link #toDomain()}.
 */
public record SearchRequestDto(

        @NotBlank
        @Pattern(regexp = "[A-Za-z]{3}", message = "El origen debe ser un código IATA de 3 letras (ej. BCN)")
        String origin,

        @NotBlank
        @Pattern(regexp = "[A-Za-z]{3}", message = "El destino debe ser un código IATA de 3 letras (ej. NRT)")
        String destination,

        @NotNull
        @Future(message = "La fecha de inicio del rango debe ser futura")
        LocalDate rangeStart,

        @NotNull
        @Future(message = "La fecha de fin del rango debe ser futura")
        LocalDate rangeEnd,

        @Min(value = 1, message = "La duración base debe ser de al menos 1 día")
        @Max(value = 365, message = "La duración base no puede superar los 365 días")
        int baseDuration,

        @Min(value = 0, message = "La variabilidad no puede ser negativa")
        @Max(value = 30, message = "La variabilidad no puede superar los 30 días")
        int variability,

        @Min(value = 0, message = "Las escalas no pueden ser negativas")
        @Max(value = 3, message = "Como máximo 3 escalas")
        int maxStops,

        @Min(value = 1, message = "topN debe ser al menos 1")
        @Max(value = 50, message = "topN no puede superar 50")
        int topN,

        // Opcional. Si no se envía, se usa EXHAUSTIVE (máxima cobertura).
        // Valores admitidos: FAST, BALANCED, EXHAUSTIVE.
        String precision,

        // Opcional. Presupuesto máximo: descarta ofertas por encima de este precio.
        // null/ausente = sin límite. Se aplica como filtro, no afecta al nº de llamadas.
        @Positive(message = "maxPrice debe ser un número positivo")
        BigDecimal maxPrice
) {
    /**
     * Convierte el DTO en el objeto de dominio, aplicando las validaciones cruzadas
     * que las anotaciones no cubren.
     *
     * @throws IllegalArgumentException si el rango es incoherente
     */
    public SearchRequest toDomain() {
        if (!rangeEnd.isAfter(rangeStart)) {
            throw new IllegalArgumentException("rangeEnd debe ser posterior a rangeStart");
        }
        long rangeDays = ChronoUnit.DAYS.between(rangeStart, rangeEnd);
        // La estancia más larga a explorar debe caber dentro del rango.
        if (baseDuration + variability >= rangeDays) {
            throw new IllegalArgumentException(
                    "El rango de vacaciones es demasiado corto para una estancia de "
                            + (baseDuration + variability) + " días");
        }
        return new SearchRequest(
                origin.toUpperCase(), destination.toUpperCase(),
                rangeStart, rangeEnd, baseDuration, variability, maxStops, topN,
                parsePrecision(), maxPrice);
    }

    /**
     * Traduce el texto {@code precision} al enum, con un mensaje claro si no es
     * válido. Si viene vacío o nulo, se usa {@link SearchPrecision#EXHAUSTIVE}.
     */
    private SearchPrecision parsePrecision() {
        if (precision == null || precision.isBlank()) {
            return SearchPrecision.EXHAUSTIVE;
        }
        try {
            return SearchPrecision.valueOf(precision.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "precision no válida: '" + precision + "'. Valores admitidos: FAST, BALANCED, EXHAUSTIVE");
        }
    }
}
