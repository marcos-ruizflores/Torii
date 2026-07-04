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
 * Motor de búsqueda con conmutación por error (failover) sobre varias fuentes.
 *
 * <p>Materializa la idea de "usar varias APIs de confianza y, cuando una se agota,
 * seguir con la siguiente". Es a su vez un {@link FlightProvider}, así que el resto
 * del sistema (caché, algoritmo) lo trata como a cualquier otra fuente — no sabe que
 * por dentro hay varias.
 *
 * <p>Comportamiento en cada consulta:
 * <ol>
 *   <li>Recorre los proveedores <b>en el orden dado</b> (los primeros, preferidos).</li>
 *   <li>Salta los que están en periodo de enfriamiento por haber agotado su cuota.</li>
 *   <li>Si un proveedor responde, devuelve su resultado sin tocar a los demás.</li>
 *   <li>Si lanza {@link ProviderQuotaExceededException}, lo "aparca" durante un
 *       {@code cooldown} y prueba el siguiente.</li>
 *   <li>Si lanza {@link FlightProviderException} (fallo temporal), prueba el
 *       siguiente sin aparcarlo.</li>
 *   <li>Si ninguno responde, lanza {@link FlightProviderException}.</li>
 * </ol>
 *
 * <p>El estado de "aparcados" se comparte entre consultas (es un bean único): así,
 * una vez Amadeus agota su cuota, las siguientes cientos de consultas de la misma
 * búsqueda no vuelven a intentarlo, van directas a la siguiente API.
 */
public class FailoverFlightProvider implements FlightProvider {

    private static final Logger log = LoggerFactory.getLogger(FailoverFlightProvider.class);

    private final List<FlightProvider> providers;
    private final Clock clock;
    private final Duration cooldown;

    /** Proveedor (por nombre) → instante hasta el que está aparcado por cuota agotada. */
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
                continue; // sabemos que está agotado; ni lo intentamos
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
