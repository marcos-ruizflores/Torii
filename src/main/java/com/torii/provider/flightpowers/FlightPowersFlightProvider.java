package com.torii.provider.flightpowers;

import com.torii.config.FlightPowersProperties;
import com.torii.model.FlightOffer;
import com.torii.model.PriceInsight;
import com.torii.provider.FlightProvider;
import com.torii.provider.FlightProviderException;
import com.torii.provider.ProviderQuotaExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FlightPowers provider: Google Flights data served through RapidAPI.
 *
 * <p>Goes first in the failover chain because it's the cheapest source per call. Like
 * the others it implements {@link FlightProvider} and maps its errors to our two
 * exceptions:
 * <ul>
 *   <li>429 (rate limit or monthly quota) and 403 (not subscribed / plan exhausted on
 *       RapidAPI) become {@link ProviderQuotaExceededException}, so the failover parks
 *       it for the cooldown.</li>
 *   <li>A {@code X-Search-Status: degraded} response means Google blocked or cut the
 *       search short. The results can't be trusted to include the cheapest fare, so
 *       it's treated as a temporary failure and the next provider gets a go.</li>
 * </ul>
 *
 * <p>It's also the only source that returns Google's price verdict (low / typical /
 * high), which ends up in {@link FlightOffer#priceInsight()}.
 */
public class FlightPowersFlightProvider implements FlightProvider {

    private static final Logger log = LoggerFactory.getLogger(FlightPowersFlightProvider.class);

    /** Unrecognized verdict values already logged, so each shows up once. */
    private static final Set<String> UNKNOWN_LEVELS = ConcurrentHashMap.newKeySet();

    private static final String ROUND_TRIP_PATH = "/api/google_flights/roundtrip/v1";
    private static final String FALLBACK_BOOKING_URL = "https://www.google.com/travel/flights";

    /** "5:05 PM" out of "5:05 PM on Tue, Oct 6". */
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    /** An IATA code inside parentheses, e.g. "Shenzhen (SZX)". */
    private static final Pattern IATA_IN_PARENS = Pattern.compile("\\(([A-Z]{3})\\)");
    private static final Pattern BARE_IATA = Pattern.compile("^[A-Z]{3}$");

    private final RestClient restClient;
    private final FlightPowersProperties props;

    public FlightPowersFlightProvider(RestClient.Builder builder, FlightPowersProperties props) {
        this.restClient = builder.baseUrl(props.baseUrl()).build();
        this.props = props;
    }

    @Override
    public List<FlightOffer> searchOffers(
            String origin, String destination,
            LocalDate departDate, LocalDate returnDate, int maxStops) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from_airport", origin);
        body.put("to_airport", destination);
        body.put("departure_date", departDate.toString());
        body.put("return_date", returnDate.toString());
        // maxStops applies to both legs, same meaning as for the other providers.
        body.put("max_departure_stops", maxStops);
        body.put("max_return_stops", maxStops);
        body.put("currency", props.currency().toLowerCase(Locale.ROOT));
        body.put("sort_type", "Price");
        body.put("passengers", List.of(1));
        body.put("limit", props.maxResults());

        try {
            ResponseEntity<List<FlightPowersRoundTripOffer>> response = restClient.post()
                    .uri(ROUND_TRIP_PATH)
                    .header("x-rapidapi-key", props.apiKey())
                    .header("x-rapidapi-host", props.rapidApiHost())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<>() {});

            List<FlightPowersRoundTripOffer> results =
                    response.getBody() != null ? response.getBody() : List.of();
            String status = response.getHeaders().getFirst("X-Search-Status");

            if ("degraded".equalsIgnoreCase(status)
                    || ("partial".equalsIgnoreCase(status) && results.isEmpty())) {
                throw new FlightProviderException("FlightPowers devolvió una búsqueda incompleta (" + status + ")");
            }
            if ("partial".equalsIgnoreCase(status)) {
                log.debug("FlightPowers: resultados parciales para {}->{} {}/{}",
                        origin, destination, departDate, returnDate);
            }

            if (!results.isEmpty() && results.stream().allMatch(r -> r == null || r.priceLevel() == null)) {
                log.info("FlightPowers: sin veredicto de precio para {}->{} {}/{}",
                        origin, destination, departDate, returnDate);
            }
            return mapToOffers(results, departDate, returnDate);

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ProviderQuotaExceededException("FlightPowers ha agotado su cuota o su ritmo (429)", e);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new ProviderQuotaExceededException("FlightPowers sin suscripción activa o sin cuota (403)", e);
        } catch (RestClientException e) {
            throw new FlightProviderException("Error consultando FlightPowers: " + e.getMessage(), e);
        }
    }

    private List<FlightOffer> mapToOffers(List<FlightPowersRoundTripOffer> results,
                                          LocalDate departDate, LocalDate returnDate) {
        return results.stream()
                .filter(Objects::nonNull)
                .filter(r -> r.totalPrice() != null)
                .map(r -> toFlightOffer(r, departDate, returnDate))
                // sort_type=Price should already do this, but the top N has to be the
                // cheapest no matter what the API decides.
                .sorted(Comparator.comparing(FlightOffer::price))
                .limit(props.maxResults())
                .toList();
    }

    private FlightOffer toFlightOffer(FlightPowersRoundTripOffer r, LocalDate departDate, LocalDate returnDate) {
        List<String> stopovers = extractStopovers(r.departureStopsInfo());
        int stops = r.departureStops() != null ? r.departureStops()
                : r.totalStops() != null ? r.totalStops()
                : stopovers.size();
        String airline = (r.departureAirline() != null && !r.departureAirline().isBlank())
                ? r.departureAirline().strip()
                : "??";
        String bookingUrl = (r.buyLink() != null && !r.buyLink().isBlank()) ? r.buyLink() : FALLBACK_BOOKING_URL;

        return new FlightOffer(airline, BigDecimal.valueOf(r.totalPrice()), props.currency(), stops,
                departDate, returnDate,
                parseTime(r.departureDescription()), parseTime(r.returnDepartureDescription()),
                stopovers, bookingUrl, toPriceInsight(r));
    }

    private static PriceInsight toPriceInsight(FlightPowersRoundTripOffer r) {
        String level = normalizeLevel(r.priceLevel());
        if (level == null) {
            return null;
        }
        return new PriceInsight(level,
                r.priceInsightsLow() != null ? BigDecimal.valueOf(r.priceInsightsLow()) : null,
                r.priceInsightsHigh() != null ? BigDecimal.valueOf(r.priceInsightsHigh()) : null);
    }

    /**
     * Maps Google's wording to low / typical / high. The exact values aren't
     * documented, so it accepts the obvious variants and logs anything else once,
     * to find out what the API really sends.
     */
    static String normalizeLevel(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.strip().toLowerCase(Locale.ROOT);
        if (v.contains("low") || v.contains("cheap") || v.contains("below")) return "low";
        if (v.contains("high") || v.contains("expensive") || v.contains("above")) return "high";
        if (v.contains("typical") || v.contains("normal") || v.contains("average")) return "typical";
        if (UNKNOWN_LEVELS.add(v)) {
            log.warn("FlightPowers: veredicto de precio desconocido \"{}\", se ignora", raw);
        }
        return null;
    }

    /**
     * "5:05 PM on Tue, Oct 6" -> 17:05. Google puts a narrow no-break space before
     * AM/PM, so those get normalized first. Returns null if the format ever changes.
     */
    static LocalTime parseTime(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.replace(' ', ' ').replace(' ', ' ').strip();
        int on = normalized.indexOf(" on ");
        String time = (on > 0 ? normalized.substring(0, on) : normalized).toUpperCase(Locale.ROOT);
        try {
            return LocalTime.parse(time, TIME_FORMAT);
        } catch (RuntimeException e) {
            return null; // if the format ever changes, null is better than breaking the search
        }
    }

    /** IATA codes of the outbound stopovers, from whatever shape the items come in. */
    private static List<String> extractStopovers(List<Object> stopsInfo) {
        if (stopsInfo == null) {
            return List.of();
        }
        return stopsInfo.stream()
                .map(FlightPowersFlightProvider::findIata)
                .filter(Objects::nonNull)
                .toList();
    }

    private static String findIata(Object item) {
        if (item instanceof String s) {
            return iataFrom(s);
        }
        if (item instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                if (value instanceof String s) {
                    String code = iataFrom(s);
                    if (code != null) {
                        return code;
                    }
                }
            }
        }
        return null;
    }

    private static String iataFrom(String text) {
        String s = text.strip();
        if (BARE_IATA.matcher(s).matches()) {
            return s;
        }
        Matcher m = IATA_IN_PARENS.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    @Override
    public String name() {
        return "GoogleFlights(FlightPowers)";
    }
}
