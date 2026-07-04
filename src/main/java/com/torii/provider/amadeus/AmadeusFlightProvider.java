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
 * Proveedor de vuelos respaldado por la API de Amadeus (Flight Offers Search).
 *
 * <p>Implementa la misma interfaz {@link FlightProvider} que el mock, así que encaja
 * sin más en el motor de failover y bajo la caché. Por cada par de fechas:
 * <ol>
 *   <li>obtiene un token válido (de {@link AmadeusAuthClient});</li>
 *   <li>llama a {@code GET /v2/shopping/flight-offers};</li>
 *   <li>mapea la respuesta a una lista de {@link FlightOffer};</li>
 *   <li>traduce los errores: un 429 se convierte en
 *       {@link ProviderQuotaExceededException} (que hace que el failover lo aparque),
 *       y cualquier otro fallo en {@link FlightProviderException}.</li>
 * </ol>
 */
public class AmadeusFlightProvider implements FlightProvider {

    private static final Logger log = LoggerFactory.getLogger(AmadeusFlightProvider.class);

    private final RestClient restClient;
    private final Supplier<String> accessTokenSupplier;
    private final AmadeusProperties props;

    /**
     * @param builder             constructor de RestClient (se le fija la baseUrl de Amadeus)
     * @param accessTokenSupplier fuente del token Bearer (normalmente {@code authClient::currentToken})
     * @param props               configuración (moneda, nº de resultados…)
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
                            // Si el usuario pidió 0 escalas, le decimos a Amadeus que filtre directos.
                            .queryParam("nonStop", maxStops == 0)
                            .build())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(AmadeusSearchResponse.class);

            return mapToOffers(response);

        } catch (HttpClientErrorException.TooManyRequests e) {
            // 429: cuota/ritmo agotado → el failover lo aparcará y pasará al siguiente.
            throw new ProviderQuotaExceededException("Amadeus ha agotado su cuota (429)", e);
        } catch (RestClientException e) {
            // Timeouts, 5xx, 401, parseo… fallo temporal: el failover probará otra fuente.
            throw new FlightProviderException("Error consultando Amadeus: " + e.getMessage(), e);
        }
    }

    /** Convierte la respuesta de Amadeus en nuestra lista de ofertas. */
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

        // Escalas = (segmentos − 1) sumados sobre todos los trayectos.
        int stops = itineraries.stream().mapToInt(it -> it.segments().size() - 1).sum();

        String airline = (offer.validatingAirlineCodes() != null && !offer.validatingAirlineCodes().isEmpty())
                ? offer.validatingAirlineCodes().get(0)
                : "??";

        BigDecimal price = new BigDecimal(offer.price().grandTotal());

        LocalDateTime firstDeparture = LocalDateTime.parse(
                outbound.segments().get(0).departure().at()); // ej. "2026-07-01T10:30:00"
        LocalDate departDate = firstDeparture.toLocalDate();
        LocalTime departureTime = firstDeparture.toLocalTime();
        LocalDateTime returnDeparture = LocalDateTime.parse(
                inbound.segments().get(0).departure().at());
        LocalDate returnDate = returnDeparture.toLocalDate();
        LocalTime returnDepartureTime = returnDeparture.toLocalTime();

        // Escalas de la IDA: los aeropuertos de llegada de todos los segmentos menos
        // el último (cada llegada intermedia es una escala).
        List<AmadeusSearchResponse.Segment> outSegments = outbound.segments();
        List<String> stopovers = outSegments.stream()
                .limit(outSegments.size() - 1)
                .map(seg -> seg.arrival().iataCode())
                .toList();

        // Amadeus no da un enlace de reserva en esta llamada; se obtendría con la
        // Flight Offers Price/Booking API. De momento dejamos un enlace informativo.
        String bookingUrl = "https://www.amadeus.com/";

        return new FlightOffer(airline, price, offer.price().currency(), stops,
                departDate, returnDate, departureTime, returnDepartureTime, stopovers, bookingUrl);
    }

    @Override
    public String name() {
        return "Amadeus";
    }
}
