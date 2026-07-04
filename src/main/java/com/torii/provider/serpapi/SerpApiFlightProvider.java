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
 * Proveedor "Google Flights" a través de SerpApi.
 *
 * <p>Implementa la misma interfaz {@link FlightProvider} que Amadeus y el mock, así
 * que encaja en el motor de failover sin cambios. Más simple que Amadeus: la
 * autenticación es solo la {@code api_key} en la URL, sin token OAuth.
 *
 * <p>Como SerpApi devuelve el precio total del viaje de ida y vuelta para el par de
 * fechas consultado, usamos las fechas que ya conocemos (las de la petición) en lugar
 * de intentar deducirlas de la respuesta, que para ida y vuelta requeriría una
 * segunda llamada.
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
                            // type=1: ida y vuelta. El "price" que devuelve YA es el
                            // total del round trip, así que con una sola llamada nos
                            // basta para conocer el precio (los detalles del tramo de
                            // vuelta requerirían una 2ª llamada con departure_token).
                            .queryParam("type", 1)
                            .queryParam("stops", toSerpApiStops(maxStops))
                            // sort_by=2 (precio): así el primer resultado de cada fecha
                            // es el MÁS BARATO, que es justo lo que Torii busca. Con el
                            // default (top flights) podríamos perdernos el mínimo real.
                            .queryParam("sort_by", 2)
                            .queryParam("currency", props.currency())
                            .queryParam("hl", "en")
                            .queryParam("api_key", props.apiKey())
                            .build())
                    .retrieve()
                    .body(SerpApiFlightsResponse.class);

            // SerpApi señala "sin búsquedas disponibles" u otros fallos en un campo "error".
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

        // Las escalas son las paradas intermedias (layovers).
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

    /** Saca la hora de salida del primer tramo. El formato de SerpApi es "AAAA-MM-DD HH:MM". */
    private LocalTime extractDepartureTime(SerpApiFlightsResponse.Leg leg) {
        if (leg == null || leg.departureAirport() == null || leg.departureAirport().time() == null) {
            return null;
        }
        try {
            String time = leg.departureAirport().time(); // "2026-09-01 10:45"
            return LocalTime.parse(time.substring(11)); // "10:45"
        } catch (RuntimeException e) {
            return null; // si el formato cambia, mejor null que romper la búsqueda
        }
    }

    /**
     * Traduce nuestro "máximo de escalas" al parámetro {@code stops} de SerpApi, que
     * usa otra codificación: 1=directo, 2=hasta 1 escala, 3=hasta 2 escalas, 0=cualquiera.
     */
    private static int toSerpApiStops(int maxStops) {
        return switch (maxStops) {
            case 0 -> 1;  // solo directos
            case 1 -> 2;  // 1 escala o menos
            case 2 -> 3;  // 2 escalas o menos
            default -> 0; // cualquier número de escalas
        };
    }

    @Override
    public String name() {
        return "GoogleFlights(SerpApi)";
    }
}
