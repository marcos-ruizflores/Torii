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
 * FlightAPI.io provider ({@code /roundtrip} endpoint).
 *
 * <p>Plugs into the failover chain like Amadeus and SerpApi: implements
 * {@link FlightProvider} and maps its errors to our two exceptions (quota exceeded
 * vs. temporary failure). Quirks of this API:
 * <ul>
 *   <li>Every parameter goes in the <b>path</b>, API key included:
 *       {@code /roundtrip/<key>/<origin>/<destination>/<out>/<back>/1/0/0/Economy/EUR}.
 *       So be careful never to dump the URL into the logs.</li>
 *   <li>Each request costs <b>2 credits</b> of the plan.</li>
 *   <li>There's no max stops parameter, so {@code maxStops} is filtered here on the
 *       client side, based on the outbound leg.</li>
 * </ul>
 */
public class FlightApiFlightProvider implements FlightProvider {

    private static final Logger log = LoggerFactory.getLogger(FlightApiFlightProvider.class);

    /** Deep links in the response are relative Skyscanner paths. */
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
                    // Everything goes in the path: 1 adult, 0 children, 0 infants.
                    .uri("/roundtrip/{key}/{origin}/{destination}/{depart}/{return}/1/0/0/{cabin}/{currency}",
                            props.apiKey(), origin, destination, departDate, returnDate,
                            props.cabinClass(), props.currency())
                    .retrieve()
                    .body(FlightApiRoundTripResponse.class);

            return mapToOffers(response, departDate, returnDate, maxStops);

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ProviderQuotaExceededException("FlightAPI ha agotado su ritmo (429)", e);
        } catch (HttpClientErrorException.Forbidden e) {
            // FlightAPI answers with a 4xx when the plan runs out of credits. Treat 403
            // as quota exceeded so the failover parks it for the cooldown.
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

        // id -> object lookups to resolve the references in the normalized model.
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
                // The API doesn't guarantee price order. Sort BEFORE limiting so the
                // top N really are the cheapest.
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
            return null; // no price, nothing to compare
        }

        // First leg_id is the OUTBOUND leg (airline, time and stops), the last one is
        // the RETURN leg (we only need its departure time).
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

    /** Cheapest price of the itinerary, falling back to the lowest pricing option. */
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

    /** Leg times are ISO ("2026-07-01T10:45:00"), so they parse directly. */
    private LocalTime extractDepartureTime(FlightApiRoundTripResponse.Leg leg) {
        if (leg == null || leg.departure() == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(leg.departure()).toLocalTime();
        } catch (RuntimeException e) {
            return null; // if the format ever changes, null is better than breaking the search
        }
    }

    /** IATA codes of the outbound stops, resolving the IDs against {@code places}. */
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

    /** First available deep link, turned into an absolute Skyscanner URL. */
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
     * Masks the API key in error messages. Since it's part of the path, any exception
     * that includes the URL would otherwise leak it into the logs.
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
