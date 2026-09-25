package com.torii.provider.serpapi;

import com.torii.config.SerpApiProperties;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Google Flights provider through SerpApi.
 *
 * <p>Same {@link FlightProvider} interface as Amadeus and the mock, so it fits into
 * the failover chain as is. Simpler than Amadeus: auth is just the {@code api_key}
 * query param, no OAuth token.
 *
 * <p>SerpApi returns the total round-trip price for the requested dates, so we use
 * the dates we already know (from the request) instead of trying to read them from
 * the response, which for round trips would need a second call.
 */
public class SerpApiFlightProvider implements FlightProvider {

    private static final Logger log = LoggerFactory.getLogger(SerpApiFlightProvider.class);

    private final RestClient restClient;
    private final SerpApiProperties props;

    public SerpApiFlightProvider(RestClient.Builder builder, SerpApiProperties props) {
        this.restClient = builder.baseUrl(props.baseUrl()).build();
        this.props = props;
    }

    @Override
    public List<FlightOffer> searchOffers(
            String origin, String destination,
            LocalDate departDate, LocalDate returnDate, int maxStops) {

        try {
            SerpApiFlightsResponse response = restClient.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("engine", "google_flights")
                            .queryParam("departure_id", origin)
                            .queryParam("arrival_id", destination)
                            .queryParam("outbound_date", departDate)
                            .queryParam("return_date", returnDate)
                            // type=1 is round trip. The returned "price" is ALREADY the
                            // round-trip total, so one call is enough to get the price
                            // (return leg details would need a 2nd call with departure_token).
                            .queryParam("type", 1)
                            .queryParam("stops", toSerpApiStops(maxStops))
                            // sort_by=2 (price) makes the first result for each date the
                            // CHEAPEST one, which is exactly what we want. The default
                            // (top flights) could miss the real minimum.
                            .queryParam("sort_by", 2)
                            .queryParam("currency", props.currency())
                            .queryParam("hl", "en")
                            .queryParam("api_key", props.apiKey())
                            .build())
                    .retrieve()
                    .body(SerpApiFlightsResponse.class);

            // SerpApi reports "out of searches" and other failures in an "error" field.
            if (response != null && response.error() != null) {
                String msg = response.error();
                if (msg.toLowerCase().contains("run out") || msg.toLowerCase().contains("searches")) {
                    throw new ProviderQuotaExceededException("SerpApi sin cuota: " + msg);
                }
                throw new FlightProviderException("SerpApi devolvió error: " + msg);
            }

            return mapToOffers(response, departDate, returnDate);

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ProviderQuotaExceededException("SerpApi ha agotado su ritmo (429)", e);
        } catch (RestClientException e) {
            throw new FlightProviderException("Error consultando SerpApi: " + e.getMessage(), e);
        }
    }

    private List<FlightOffer> mapToOffers(SerpApiFlightsResponse response,
                                          LocalDate departDate, LocalDate returnDate) {
        if (response == null) {
            return List.of();
        }
        List<SerpApiFlightsResponse.FlightGroup> groups = new ArrayList<>();
        if (response.bestFlights() != null) {
            groups.addAll(response.bestFlights());
        }
        if (response.otherFlights() != null) {
            groups.addAll(response.otherFlights());
        }
        return groups.stream()
                .map(g -> toFlightOffer(g, departDate, returnDate))
                .limit(props.maxResults())
                .toList();
    }

    private FlightOffer toFlightOffer(SerpApiFlightsResponse.FlightGroup group,
                                      LocalDate departDate, LocalDate returnDate) {
        boolean hasLegs = group.flights() != null && !group.flights().isEmpty();
        SerpApiFlightsResponse.Leg firstLeg = hasLegs ? group.flights().get(0) : null;

        String airline = (firstLeg != null && firstLeg.airline() != null)
                ? firstLeg.airline()
                : "??";

        // Stopovers are the layovers.
        List<String> stopovers = (group.layovers() != null)
                ? group.layovers().stream().map(SerpApiFlightsResponse.Layover::id).toList()
                : List.of();
        int stops = !stopovers.isEmpty()
                ? stopovers.size()
                : (hasLegs ? Math.max(0, group.flights().size() - 1) : 0);

        BigDecimal price = group.price() != null
                ? BigDecimal.valueOf(group.price())
                : BigDecimal.ZERO;

        LocalTime departureTime = extractDepartureTime(firstLeg);

        String bookingUrl = "https://www.google.com/travel/flights";

        return new FlightOffer(airline, price, props.currency(), stops,
                departDate, returnDate, departureTime, stopovers, bookingUrl);
    }

    /** Departure time of the first leg. SerpApi uses "YYYY-MM-DD HH:MM". */
    private LocalTime extractDepartureTime(SerpApiFlightsResponse.Leg leg) {
        if (leg == null || leg.departureAirport() == null || leg.departureAirport().time() == null) {
            return null;
        }
        try {
            String time = leg.departureAirport().time(); // "2026-09-01 10:45"
            return LocalTime.parse(time.substring(11)); // "10:45"
        } catch (RuntimeException e) {
            return null; // if the format ever changes, null is better than breaking the search
        }
    }

    /**
     * Maps our max stops to SerpApi's {@code stops} param, which uses a different
     * encoding: 1 = direct, 2 = up to 1 stop, 3 = up to 2 stops, 0 = any.
     */
    private static int toSerpApiStops(int maxStops) {
        return switch (maxStops) {
            case 0 -> 1;  // direct only
            case 1 -> 2;  // 1 stop or fewer
            case 2 -> 3;  // 2 stops or fewer
            default -> 0; // any number of stops
        };
    }

    @Override
    public String name() {
        return "GoogleFlights(SerpApi)";
    }
}
