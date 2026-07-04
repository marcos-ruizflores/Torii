package com.torii.provider;

/**
 * Indica que un {@link FlightProvider} ha agotado su cuota o límite de peticiones
 * (típicamente, una respuesta HTTP 429 de la API).
 *
 * <p>A diferencia de un fallo temporal, esto significa que <b>no merece la pena
 * volver a intentarlo con este proveedor durante un rato</b>. El
 * {@link FailoverFlightProvider} lo aprovecha para "aparcar" al proveedor durante un
 * tiempo de enfriamiento (cooldown) y no malgastar llamadas que sabe que fallarán,
 * yendo directo al siguiente.
 */
public class ProviderQuotaExceededException extends FlightProviderException {

    public ProviderQuotaExceededException(String message) {
        super(message);
    }

    public ProviderQuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
