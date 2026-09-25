package com.torii.provider;

/**
 * Thrown when a {@link FlightProvider} has run out of quota or hit its rate limit
 * (usually an HTTP 429 from the API).
 *
 * <p>Unlike a temporary failure, this means <b>there's no point trying this provider
 * again for a while</b>. {@link FailoverFlightProvider} uses it to park the provider
 * for a cooldown period instead of wasting calls it knows will fail, and goes
 * straight to the next one.
 */
public class ProviderQuotaExceededException extends FlightProviderException {

    public ProviderQuotaExceededException(String message) {
        super(message);
    }

    public ProviderQuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
