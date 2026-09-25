package com.torii.config;

import com.torii.cache.TripTtlPolicy;
import com.torii.provider.CachingFlightProvider;
import com.torii.provider.FailoverFlightProvider;
import com.torii.provider.FlightProvider;
import com.torii.provider.MockFlightProvider;
import com.torii.provider.amadeus.AmadeusAuthClient;
import com.torii.provider.amadeus.AmadeusFlightProvider;
import com.torii.provider.flightapi.FlightApiFlightProvider;
import com.torii.provider.serpapi.SerpApiFlightProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Wires up the flight provider chain.
 *
 * <p>The final graph is <b>Cache -> Failover -> [enabled real providers, Mock]</b>.
 * <ul>
 *   <li>The <b>cache</b> ({@link CachingFlightProvider}, {@code @Primary}) is what the
 *       algorithm gets injected. Avoids repeating calls.</li>
 *   <li>The <b>failover</b> ({@link FailoverFlightProvider}) tries providers in order
 *       and moves on to the next one when a provider runs out of quota.</li>
 *   <li>Each real provider (Amadeus, SerpApi, FlightAPI) is only added when it's
 *       {@code enabled=true} in the config.</li>
 *   <li>{@link MockFlightProvider} always goes last as a safety net. It never fails,
 *       so the app always returns something even with no real APIs configured or all
 *       of them out of quota.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties({CacheProperties.class, AmadeusProperties.class,
        SerpApiProperties.class, FlightApiProperties.class})
public class ProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(ProviderConfig.class);

    /** How long the failover parks a provider after it runs out of quota. */
    private static final Duration FAILOVER_COOLDOWN = Duration.ofMinutes(5);

    /**
     * System clock as a bean so the TTL policy and the failover can get "now". Being a
     * bean means tests can inject a fixed clock.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /**
     * Cached provider wrapping the failover chain. {@code @Primary} makes it the one
     * the algorithm receives. The return type is {@code CachingFlightProvider} rather
     * than the interface so it can also be injected into the stats controller.
     */
    @Bean
    @Primary
    public CachingFlightProvider cachingFlightProvider(
            MockFlightProvider mock, TripTtlPolicy ttlPolicy, Clock clock,
            CacheProperties cacheProps, AmadeusProperties amadeusProps,
            SerpApiProperties serpApiProps, FlightApiProperties flightApiProps) {

        List<FlightProvider> providers = new ArrayList<>();

        if (amadeusProps.enabled()) {
            AmadeusAuthClient auth = new AmadeusAuthClient(RestClient.builder(), amadeusProps, clock);
            providers.add(new AmadeusFlightProvider(RestClient.builder(), auth::currentToken, amadeusProps));
            log.info("Proveedor Amadeus ACTIVADO");
        }
        if (serpApiProps.enabled()) {
            providers.add(new SerpApiFlightProvider(RestClient.builder(), serpApiProps));
            log.info("Proveedor Google Flights (SerpApi) ACTIVADO");
        }
        // FlightAPI goes AFTER SerpApi: its free quota is a one-off trial (~20 calls),
        // so keep it as a fallback for when SerpApi runs out.
        if (flightApiProps.enabled()) {
            providers.add(new FlightApiFlightProvider(RestClient.builder(), flightApiProps));
            log.info("Proveedor FlightAPI ACTIVADO");
        }
        providers.add(mock); // safety net: always last, never fails

        log.info("Motor de búsqueda con {} proveedor(es): {}",
                providers.size(), providers.stream().map(FlightProvider::name).toList());

        FailoverFlightProvider failover = new FailoverFlightProvider(providers, clock, FAILOVER_COOLDOWN);
        return new CachingFlightProvider(failover, ttlPolicy, clock, cacheProps.maximumSize());
    }
}
