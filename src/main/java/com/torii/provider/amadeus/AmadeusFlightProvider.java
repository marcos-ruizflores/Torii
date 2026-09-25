package com.torii.provider.amadeus;

import com.torii.config.AmadeusProperties;
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
import java.util.List;
import java.util.function.Supplier;

/**
 * Flight provider backed by the Amadeus Flight Offers Search API.
 *
 * <p>Same {@link FlightProvider} interface as the mock, so it plugs straight into the
 * failover chain and sits under the cache. For each date pair it:
 * <ol>
 *   <li>gets a valid token from {@link AmadeusAuthClient};</li>
 *   <li>calls {@code GET /v2/shopping/flight-offers};</li>
 *   <li>maps the response to a list of {@link FlightOffer};</li>
 *   <li>maps errors: a 429 becomes {@link ProviderQuotaExceededException} (so the
 *       failover parks it) and anything else becomes {@link FlightProviderException}.</li>
 * </ol>
 */
public class AmadeusFlightProvider implements FlightProvider {

    private static final Logger log = LoggerFactory.getLogger(AmadeusFlightProvider.class);

    private final RestClient restClient;
    private final Supplier<String> accessTokenSupplier;
    private final AmadeusProperties props;

    /**
     * @param builder             RestClient builder (the Amadeus baseUrl is set on it)
     * @param accessTokenSupplier where the Bearer token comes from (usually {@code authClient::currentToken})
     * @param props               config (currency, number of results, etc.)
     */
    public AmadeusFlightProvider(RestClient.Builder builder,
                                 Supplier<String> accessTokenSupplier,
                                 AmadeusProperties props) {
        this.restClient = builder.baseUrl(props.baseUrl()).build();
        this.accessTokenSupplier = accessTokenSupplier;
        this.props = props;
    }

    @Override
    public List<FlightOffer> searchOffers(
            String origin, String destination,
            LocalDate departDate, LocalDate returnDate, int maxStops) {

        String token = accessTokenSupplier.get();

        try {
            AmadeusSearchResponse response = restClient.get()
                    .uri(uri -> uri.path("/v2/shopping/flight-offers")
                            .queryParam("originLocationCode", origin)
                            .queryParam("destinationLocationCode", destination)
                            .queryParam("departureDate", departDate)
                            .queryParam("returnDate", returnDate)
                            .queryParam("adults", 1)
                            .queryParam("currencyCode", props.currency())
                            .queryParam("max", props.maxResults())
                            // If the user asked for 0 stops, let Amadeus filter to direct flights.
                            .queryParam("nonStop", maxStops == 0)
                            .build())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(AmadeusSearchResponse.class);

            return mapToOffers(response);

        } catch (HttpClientErrorException.TooManyRequests e) {
            // 429: quota or rate limit hit, the failover will park it and move on.
            throw new ProviderQuotaExceededException("Amadeus ha agotado su cuota (429)", e);
        } catch (RestClientException e) {
            // Timeouts, 5xx, 401, parsing errors... treated as temporary, failover tries another source.
            throw new FlightProviderException("Error consultando Amadeus: " + e.getMessage(), e);
        }
    }

    /** Maps the Amadeus response to our list of offers. */
    private List<FlightOffer> mapToOffers(AmadeusSearchResponse response) {
        if (response == null || response.data() == null) {
            return List.of();
        }
        return response.data().stream()
                .map(this::toFlightOffer)
                .toList();
    }

    private FlightOffer toFlightOffer(AmadeusSearchResponse.Offer offer) {
        List<AmadeusSearchResponse.Itinerary> itineraries = offer.itineraries();
        AmadeusSearchResponse.Itinerary outbound = itineraries.get(0);
        AmadeusSearchResponse.Itinerary inbound = itineraries.get(itineraries.size() - 1);

        // Stops = (segments - 1) summed over every leg.
        int stops = itineraries.stream().mapToInt(it -> it.segments().size() - 1).sum();

        String airline = (offer.validatingAirlineCodes() != null && !offer.validatingAirlineCodes().isEmpty())
                ? offer.validatingAirlineCodes().get(0)
                : "??";

        BigDecimal price = new BigDecimal(offer.price().grandTotal());

        LocalDateTime firstDeparture = LocalDateTime.parse(
                outbound.segments().get(0).departure().at()); // e.g. "2026-07-01T10:30:00"
        LocalDate departDate = firstDeparture.toLocalDate();
        LocalTime departureTime = firstDeparture.toLocalTime();
        LocalDateTime returnDeparture = LocalDateTime.parse(
                inbound.segments().get(0).departure().at());
        LocalDate returnDate = returnDeparture.toLocalDate();
        LocalTime returnDepartureTime = returnDeparture.toLocalTime();

        // Outbound stopovers: arrival airports of every segment except the last one
        // (each intermediate arrival is a stop).
        List<AmadeusSearchResponse.Segment> outSegments = outbound.segments();
        List<String> stopovers = outSegments.stream()
                .limit(outSegments.size() - 1)
                .map(seg -> seg.arrival().iataCode())
                .toList();

        // This endpoint doesn't return a booking link, that would need the Flight
        // Offers Price/Booking API. Placeholder link for now.
        String bookingUrl = "https://www.amadeus.com/";

        return new FlightOffer(airline, price, offer.price().currency(), stops,
                departDate, returnDate, departureTime, returnDepartureTime, stopovers, bookingUrl);
    }

    @Override
    public String name() {
        return "Amadeus";
    }
}
