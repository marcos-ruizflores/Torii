package com.torii.provider;

/**
 * Indica que un {@link FlightProvider} no ha podido atender una consulta.
 *
 * <p>Es la señal que el {@link FailoverFlightProvider} captura para pasar al
 * siguiente proveedor. Representa un fallo <b>temporal o recuperable</b> (timeout,
 * error 5xx, respuesta inesperada): conviene reintentar con otra fuente, pero no hay
 * motivo para dar por agotado a este proveedor.
 *
 * <p>Cada proveedor real (p. ej. el futuro {@code AmadeusFlightProvider}) debe
 * traducir sus errores a esta excepción, para que el motor de failover los entienda.
 */
public class FlightProviderException extends RuntimeException {

    public FlightProviderException(String message) {
        super(message);
    }

    public FlightProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
