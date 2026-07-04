package com.torii.provider.flightapi;

import com.torii.config.FlightApiProperties;
import com.torii.model.FlightOffer;
import com.torii.provider.FlightProvider;
import com.torii.provider.FlightProviderException;
import com.torii.provider.ProviderQuotaExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Proveedor FlightAPI.io (endpoint {@code /roundtrip}).
 *
 * <p>Encaja en el motor de failover igual que Amadeus y SerpApi: implementa
 * {@link FlightProvider} y traduce sus errores a nuestras dos excepciones (cuota
 * agotada vs. fallo temporal). Peculiaridades de esta API:
 * <ul>
 *   <li>Todos los parámetros van en el <b>path</b>, incluida la API key:
 *       {@code /roundtrip/<key>/<origen>/<destino>/<ida>/<vuelta>/1/0/0/Economy/EUR}.
 *       Por eso hay que tener cuidado de no volcar la URL en los logs.</li>
 *   <li>Cada petición cuesta <b>2 créditos</b> del plan.</li>
 *   <li>No tiene parámetro de escalas máximas, así que {@code maxStops} se filtra
 *       aquí, en cliente, sobre el tramo de ida.</li>
 * </ul>
 */
public class FlightApiFlightProvider implements FlightProvider {

    private static final Logger log = LoggerFactory.getLogger(FlightApiFlightProvider.class);

    /** Los deep-links de la respuesta son rutas relativas de Skyscanner. */
    private static final String DEEPLINK_BASE = "https://www.skyscanner.net";

    private final RestClient restClient;
    private final FlightApiProperties props;

    public FlightApiFlightProvider(RestClient.Builder builder, FlightApiProperties props) {
        this.restClient = builder.baseUrl(props.baseUrl()).build();
        this.props = props;
    }

    @Override
    public List<FlightOffer> searchOffers(
            String origin, String destination,
            LocalDate departDate, LocalDate returnDate, int maxStops) {

        try {
            FlightApiRoundTripResponse response = restClient.get()
                    // Esquema de la API: todo por path, con 1 adulto, 0 niños, 0 bebés.
                    .uri("/roundtrip/{key}/{origin}/{destination}/{depart}/{return}/1/0/0/{cabin}/{currency}",
                            props.apiKey(), origin, destination, departDate, returnDate,
                            props.cabinClass(), props.currency())
                    .retrieve()
                    .body(FlightApiRoundTripResponse.class);

            return mapToOffers(response, departDate, returnDate, maxStops);

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ProviderQuotaExceededException("FlightAPI ha agotado su ritmo (429)", e);
        } catch (HttpClientErrorException.Forbidden e) {
            // FlightAPI responde 4xx cuando el plan se queda sin créditos; tratamos el
            // 403 como cuota agotada para que el failover lo aparque con cooldown.
            throw new ProviderQuotaExceededException("FlightAPI sin créditos o key inválida (403)", e);
        } catch (RestClientException e) {
            throw new FlightProviderException("Error consultando FlightAPI: " + sanitized(e.getMessage()), e);
        }
    }

    private List<FlightOffer> mapToOffers(FlightApiRoundTripResponse response,
                                          LocalDate departDate, LocalDate returnDate, int maxStops) {
        if (response == null || response.itineraries() == null) {
            return List.of();
        }

        // Índices id → objeto para resolver las referencias del modelo normalizado.
        Map<String, FlightApiRoundTripResponse.Leg> legsById = indexBy(
                response.legs(), FlightApiRoundTripResponse.Leg::id);
        Map<Long, FlightApiRoundTripResponse.Carrier> carriersById = indexBy(
                response.carriers(), FlightApiRoundTripResponse.Carrier::id);
        Map<Long, FlightApiRoundTripResponse.Place> placesById = indexBy(
                response.places(), FlightApiRoundTripResponse.Place::id);

        return response.itineraries().stream()
                .map(it -> toFlightOffer(it, legsById, carriersById, placesById, departDate, returnDate))
                .filter(Objects::nonNull)
                .filter(offer -> offer.stops() <= maxStops)
                // La API no garantiza orden por precio; ordenamos ANTES de recortar
                // para que el top-N sea de verdad el más barato.
                .sorted(Comparator.comparing(FlightOffer::price))
                .limit(props.maxResults())
                .toList();
    }

    private FlightOffer toFlightOffer(FlightApiRoundTripResponse.Itinerary itinerary,
                                      Map<String, FlightApiRoundTripResponse.Leg> legsById,
                                      Map<Long, FlightApiRoundTripResponse.Carrier> carriersById,
                                      Map<Long, FlightApiRoundTripResponse.Place> placesById,
                                      LocalDate departDate, LocalDate returnDate) {

        BigDecimal price = extractPrice(itinerary);
        if (price == null) {
            return null; // sin precio no hay oferta que comparar
        }

        // El primer leg_id es el tramo de IDA (aerolínea, hora y escalas) y el
        // último el de VUELTA (su hora de salida).
        List<String> legIds = itinerary.legIds() != null ? itinerary.legIds() : List.of();
        FlightApiRoundTripResponse.Leg outbound = !legIds.isEmpty() ? legsById.get(legIds.get(0)) : null;
        FlightApiRoundTripResponse.Leg inbound = legIds.size() > 1
                ? legsById.get(legIds.get(legIds.size() - 1))
                : null;

        String airline = extractAirline(outbound, carriersById);
        LocalTime departureTime = extractDepartureTime(outbound);
        LocalTime returnDepartureTime = extractDepartureTime(inbound);
        List<String> stopovers = extractStopovers(outbound, placesById);
        int stops = (outbound != null && outbound.stopCount() != null)
                ? outbound.stopCount()
                : stopovers.size();

        return new FlightOffer(airline, price, props.currency(), stops, departDate, returnDate,
                departureTime, returnDepartureTime, stopovers, extractBookingUrl(itinerary));
    }

    /** Precio más barato del itinerario, con fallback al mínimo de sus opciones. */
    private BigDecimal extractPrice(FlightApiRoundTripResponse.Itinerary itinerary) {
        if (itinerary.cheapestPrice() != null && itinerary.cheapestPrice().amount() != null) {
            return BigDecimal.valueOf(itinerary.cheapestPrice().amount());
        }
        if (itinerary.pricingOptions() == null) {
            return null;
        }
        return itinerary.pricingOptions().stream()
                .map(FlightApiRoundTripResponse.PricingOption::price)
                .filter(Objects::nonNull)
                .map(FlightApiRoundTripResponse.Price::amount)
                .filter(Objects::nonNull)
                .min(Double::compareTo)
                .map(BigDecimal::valueOf)
                .orElse(null);
    }

    private String extractAirline(FlightApiRoundTripResponse.Leg leg,
                                  Map<Long, FlightApiRoundTripResponse.Carrier> carriersById) {
        if (leg == null || leg.marketingCarrierIds() == null || leg.marketingCarrierIds().isEmpty()) {
            return "??";
        }
        FlightApiRoundTripResponse.Carrier carrier = carriersById.get(leg.marketingCarrierIds().get(0));
        return (carrier != null && carrier.name() != null) ? carrier.name() : "??";
    }

    /** El formato del leg es ISO ("2026-07-01T10:45:00"): parse directo. */
    private LocalTime extractDepartureTime(FlightApiRoundTripResponse.Leg leg) {
        if (leg == null || leg.departure() == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(leg.departure()).toLocalTime();
        } catch (RuntimeException e) {
            return null; // si el formato cambia, mejor null que romper la búsqueda
        }
    }

    /** Códigos IATA de las escalas de la ida, resolviendo los IDs contra {@code places}. */
    private List<String> extractStopovers(FlightApiRoundTripResponse.Leg leg,
                                          Map<Long, FlightApiRoundTripResponse.Place> placesById) {
        if (leg == null || leg.stopIds() == null) {
            return List.of();
        }
        return leg.stopIds().stream()
                .flatMap(List::stream)
                .map(placesById::get)
                .filter(Objects::nonNull)
                .map(FlightApiRoundTripResponse.Place::bestCode)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Primer deep-link disponible, convertido en URL absoluta de Skyscanner. */
    private String extractBookingUrl(FlightApiRoundTripResponse.Itinerary itinerary) {
        if (itinerary.pricingOptions() != null) {
            for (FlightApiRoundTripResponse.PricingOption option : itinerary.pricingOptions()) {
                if (option.items() == null) continue;
                for (FlightApiRoundTripResponse.Item item : option.items()) {
                    if (item.url() != null && !item.url().isBlank()) {
                        return item.url().startsWith("/") ? DEEPLINK_BASE + item.url() : item.url();
                    }
                }
            }
        }
        return DEEPLINK_BASE;
    }

    /**
     * Oculta la API key en los mensajes de error: como viaja en el path, cualquier
     * excepción que incluya la URL la dejaría escrita en los logs.
     */
    private String sanitized(String message) {
        if (message == null || props.apiKey().isBlank()) {
            return message;
        }
        return message.replace(props.apiKey(), "***");
    }

    private static <K, V> Map<K, V> indexBy(List<V> items, Function<V, K> keyExtractor) {
        if (items == null) {
            return Map.of();
        }
        return items.stream()
                .filter(item -> keyExtractor.apply(item) != null)
                .collect(java.util.stream.Collectors.toMap(keyExtractor, Function.identity(), (a, b) -> a));
    }

    @Override
    public String name() {
        return "FlightAPI";
    }
}
