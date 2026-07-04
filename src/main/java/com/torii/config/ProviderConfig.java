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
 * Aquí "montamos las piezas": le decimos a Spring cómo construir el grafo de
 * proveedores de vuelos.
 *
 * <p>El grafo final es: <b>Caché → Failover → [proveedores reales activos, Mock]</b>.
 * <ul>
 *   <li>La <b>caché</b> ({@link CachingFlightProvider}, {@code @Primary}) es lo que
 *       recibe el algoritmo; evita repetir llamadas.</li>
 *   <li>El <b>failover</b> ({@link FailoverFlightProvider}) prueba los proveedores en
 *       orden y, si uno agota su cuota, pasa al siguiente.</li>
 *   <li>Cada proveedor real (Amadeus, SerpApi) se incluye solo si está
 *       {@code enabled=true} en la configuración.</li>
 *   <li>El {@link MockFlightProvider} va siempre el último, como red de seguridad:
 *       nunca falla, así que la app siempre devuelve algo aunque no haya APIs reales
 *       configuradas o todas estén agotadas.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties({CacheProperties.class, AmadeusProperties.class,
        SerpApiProperties.class, FlightApiProperties.class})
public class ProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(ProviderConfig.class);

    /** Tiempo que el failover aparca a un proveedor tras agotar su cuota. */
    private static final Duration FAILOVER_COOLDOWN = Duration.ofMinutes(5);

    /**
     * Reloj del sistema como bean, para que la política de TTL y el failover puedan
     * calcular "ahora". Tenerlo como bean permite inyectar un reloj fijo en tests.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /**
     * El proveedor con caché, que envuelve al motor de failover. {@code @Primary}
     * hace que sea el que reciba el algoritmo. El tipo de retorno es
     * {@code CachingFlightProvider} (no la interfaz) para poder inyectarlo también en
     * el controlador de estadísticas.
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
        // FlightAPI va DESPUÉS de SerpApi: su cuota gratis es una prueba única
        // (~20 llamadas), así que lo reservamos como respaldo cuando SerpApi se agote.
        if (flightApiProps.enabled()) {
            providers.add(new FlightApiFlightProvider(RestClient.builder(), flightApiProps));
            log.info("Proveedor FlightAPI ACTIVADO");
        }
        providers.add(mock); // red de seguridad: siempre el último y nunca falla

        log.info("Motor de búsqueda con {} proveedor(es): {}",
                providers.size(), providers.stream().map(FlightProvider::name).toList());

        FailoverFlightProvider failover = new FailoverFlightProvider(providers, clock, FAILOVER_COOLDOWN);
        return new CachingFlightProvider(failover, ttlPolicy, clock, cacheProps.maximumSize());
    }
}
