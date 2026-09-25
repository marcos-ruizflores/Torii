package com.torii.provider;

/**
 * Thrown when a {@link FlightProvider} couldn't handle a lookup.
 *
 * <p>{@link FailoverFlightProvider} catches it to move on to the next provider. It
 * means a <b>temporary or recoverable</b> failure (timeout, 5xx, unexpected
 * response): worth retrying with another source, but no reason to treat this
 * provider as exhausted.
 *
 * <p>Every real provider should map its own errors to this exception so the
 * failover logic can understand them.
 */
public class FlightProviderException extends RuntimeException {

    public FlightProviderException(String message) {
        super(message);
    }

    public FlightProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
