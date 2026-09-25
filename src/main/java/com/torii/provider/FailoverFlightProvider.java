package com.torii.provider;

import com.torii.model.FlightOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Failover over several flight data sources.
 *
 * <p>The idea: use a few APIs we trust and, when one runs out of quota, move on to
 * the next. It's also a {@link FlightProvider}, so the cache and the algorithm treat
 * it like any other source without knowing there are several behind it.
 *
 * <p>On each lookup:
 * <ol>
 *   <li>Go through the providers <b>in the given order</b> (first ones are preferred).</li>
 *   <li>Skip the ones that are cooling down after running out of quota.</li>
 *   <li>If a provider answers, return that and don't touch the rest.</li>
 *   <li>If it throws {@link ProviderQuotaExceededException}, park it for
 *       {@code cooldown} and try the next one.</li>
 *   <li>If it throws {@link FlightProviderException} (temporary failure), try the next
 *       one without parking it.</li>
 *   <li>If nobody answers, throw {@link FlightProviderException}.</li>
 * </ol>
 *
 * <p>The parked state is shared across lookups (this is a singleton bean). Once
 * Amadeus runs out of quota, the next few hundred lookups of the same search don't
 * bother trying it again and go straight to the next API.
 */
public class FailoverFlightProvider implements FlightProvider {

    private static final Logger log = LoggerFactory.getLogger(FailoverFlightProvider.class);

    private final List<FlightProvider> providers;
    private final Clock clock;
    private final Duration cooldown;

    /** Provider name -> instant until which it's parked for running out of quota. */
    private final Map<String, Instant> parkedUntil = new ConcurrentHashMap<>();

    public FailoverFlightProvider(List<FlightProvider> providers, Clock clock, Duration cooldown) {
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("El failover necesita al menos un proveedor");
        }
        this.providers = List.copyOf(providers);
        this.clock = clock;
        this.cooldown = cooldown;
    }

    @Override
    public List<FlightOffer> searchOffers(
            String origin, String destination,
            LocalDate departDate, LocalDate returnDate, int maxStops) {

        Instant now = clock.instant();

        for (FlightProvider provider : providers) {
            if (isParked(provider, now)) {
                continue; // we know it's out of quota, don't even try
            }
            try {
                return provider.searchOffers(origin, destination, departDate, returnDate, maxStops);
            } catch (ProviderQuotaExceededException e) {
                Instant until = now.plus(cooldown);
                parkedUntil.put(provider.name(), until);
                log.warn("Proveedor '{}' agotó su cuota; aparcado hasta {}. Pruebo el siguiente.",
                        provider.name(), until);
            } catch (FlightProviderException e) {
                log.warn("Proveedor '{}' con fallo temporal ({}). Pruebo el siguiente.",
                        provider.name(), e.getMessage());
            }
        }

        throw new FlightProviderException(
                "Ningún proveedor pudo atender la búsqueda " + origin + "->" + destination
                        + " (" + departDate + " / " + returnDate + ")");
    }

    private boolean isParked(FlightProvider provider, Instant now) {
        Instant until = parkedUntil.get(provider.name());
        return until != null && now.isBefore(until);
    }

    @Override
    public String name() {
        return "Failover" + providers.stream().map(FlightProvider::name).toList();
    }
}
