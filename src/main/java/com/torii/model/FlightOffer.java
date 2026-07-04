package com.torii.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Una oferta concreta de vuelo ida y vuelta para un par de fechas determinado.
 *
 * <p>Es lo que un {@link com.torii.provider.FlightProvider} devuelve para una
 * combinación (origen, destino, fecha de ida, fecha de vuelta) y, a la vez, lo que
 * el algoritmo selecciona como mejores resultados.
 *
 * <p>Notas de diseño:
 * <ul>
 *   <li>El precio es {@link BigDecimal}: nunca se usa {@code double} para dinero,
 *       porque introduce errores de redondeo.</li>
 *   <li>La duración de la estancia no se guarda como campo: se deriva de las fechas
 *       con {@link #durationDays()}, evitando datos redundantes que puedan
 *       contradecirse.</li>
 *   <li>{@code departureTime}, {@code returnDepartureTime} y {@code stopovers}
 *       enriquecen la oferta para poder distinguir vuelos que, de otro modo,
 *       parecerían iguales (misma aerolínea y precio pero distinto horario o
 *       escala). Pueden faltar ({@code null} / lista vacía) si la fuente no los
 *       aporta — p. ej. SerpApi no da la hora de vuelta sin una segunda llamada.</li>
 * </ul>
 */
public record FlightOffer(
        String airline,
        BigDecimal price,
        String currency,
        int stops,
        LocalDate departDate,
        LocalDate returnDate,
        LocalTime departureTime,
        LocalTime returnDepartureTime,
        List<String> stopovers,
        String bookingUrl
) {
    /**
     * Constructor de conveniencia sin datos de enriquecimiento (horas y escalas),
     * para el código y los tests que no los necesitan.
     */
    public FlightOffer(String airline, BigDecimal price, String currency, int stops,
                       LocalDate departDate, LocalDate returnDate, String bookingUrl) {
        this(airline, price, currency, stops, departDate, returnDate, null, null, List.of(), bookingUrl);
    }

    /**
     * Constructor de compatibilidad SIN hora de vuelta, para las fuentes que no
     * pueden aportarla (SerpApi) y los tests previos a este campo.
     */
    public FlightOffer(String airline, BigDecimal price, String currency, int stops,
                       LocalDate departDate, LocalDate returnDate, LocalTime departureTime,
                       List<String> stopovers, String bookingUrl) {
        this(airline, price, currency, stops, departDate, returnDate,
                departureTime, null, stopovers, bookingUrl);
    }

    /** Días de estancia (diferencia exacta entre vuelta e ida). */
    public long durationDays() {
        return ChronoUnit.DAYS.between(departDate, returnDate);
    }
}
