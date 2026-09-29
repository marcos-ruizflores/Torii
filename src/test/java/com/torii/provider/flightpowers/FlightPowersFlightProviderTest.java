package com.torii.provider.flightpowers;

import com.torii.config.FlightPowersProperties;
import com.torii.model.FlightOffer;
import com.torii.provider.FlightProviderException;
import com.torii.provider.ProviderQuotaExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * FlightPowers provider test against a mock HTTP server. Checks the request (RapidAPI
 * headers and JSON body), the mapping of the flattened response to {@link FlightOffer}
 * (including Google's price verdict), sorting by price, and that 429 / degraded
 * responses end up as the right exception for the failover.
 */
class FlightPowersFlightProviderTest {

    private static final String URL =
            "https://google-flights-live-api.p.rapidapi.com/api/google_flights/roundtrip/v1";

    /**
     * Two results OUT OF ORDER on purpose: a pricier direct one and a cheaper one via
     * Shenzhen. The times use the narrow no-break space Google puts before AM/PM.
     */
    private static final String SAMPLE_JSON = """
            [
              {
                "total_price": "€912",
                "total_price_as_number": 912,
                "total_stops": 0,
                "buy_link": "https://www.google.com/travel/flights?tfs=DIRECT",
                "departure_flight_airline": "Iberia",
                "departure_flight_stops": 0,
                "departure_flight_departure_description": "11:40 AM on Mon, Nov 16",
                "departure_stops_info": [],
                "return_flight_departure_description": "9:15 PM on Mon, Nov 30",
                "price_range_in_relation_to_other_periods": "high",
                "price_insights_low": 610,
                "price_insights_high": 1050
              },
              {
                "total_price": "€755",
                "total_price_as_number": 755,
                "total_stops": 2,
                "buy_link": "https://www.google.com/travel/flights?tfs=CHEAP",
                "departure_flight_airline": "China Southern",
                "departure_flight_stops": 1,
                "departure_flight_departure_description": "5:05 PM on Mon, Nov 16",
                "departure_stops_info": [ {"airport": "Shenzhen Bao'an International (SZX)", "duration": "3 hr"} ],
                "return_flight_departure_description": "8:50 AM on Mon, Nov 30",
                "price_range_in_relation_to_other_periods": "typical",
                "price_insights_low": 610,
                "price_insights_high": 1050
              }
            ]
            """;

    private MockRestServiceServer server;
    private FlightPowersFlightProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new FlightPowersFlightProvider(builder, FlightPowersProperties.defaults().withApiKey("TESTKEY"));
    }

    @Test
    void mapeaLaRespuestaYOrdenaPorPrecio() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-rapidapi-key", "TESTKEY"))
                .andExpect(header("x-rapidapi-host", "google-flights-live-api.p.rapidapi.com"))
                .andExpect(jsonPath("$.from_airport").value("BCN"))
                .andExpect(jsonPath("$.to_airport").value("NRT"))
                .andExpect(jsonPath("$.departure_date").value("2026-11-16"))
                .andExpect(jsonPath("$.return_date").value("2026-11-30"))
                .andExpect(jsonPath("$.max_departure_stops").value(2))
                .andExpect(jsonPath("$.max_return_stops").value(2))
                .andExpect(jsonPath("$.currency").value("eur"))
                .andExpect(jsonPath("$.sort_type").doesNotExist()) // price sort drops fares and the verdict
                .andExpect(jsonPath("$.limit").value(25))
                .andExpect(jsonPath("$.max_price").doesNotExist())
                .andRespond(withSuccess(SAMPLE_JSON, APPLICATION_JSON));

        List<FlightOffer> offers = provider.searchOffers(
                "BCN", "NRT", LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 30), 2);

        assertThat(offers).hasSize(2);

        // Cheapest first, even though it was second in the JSON.
        FlightOffer cheapest = offers.get(0);
        assertThat(cheapest.airline()).isEqualTo("China Southern");
        assertThat(cheapest.price()).isEqualByComparingTo("755");
        assertThat(cheapest.currency()).isEqualTo("EUR");
        assertThat(cheapest.stops()).isEqualTo(1); // outbound stops, not the total
        assertThat(cheapest.stopovers()).containsExactly("SZX");
        assertThat(cheapest.departureTime()).isEqualTo(LocalTime.of(17, 5));
        assertThat(cheapest.returnDepartureTime()).isEqualTo(LocalTime.of(8, 50));
        assertThat(cheapest.departDate()).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(cheapest.returnDate()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(cheapest.bookingUrl()).isEqualTo("https://www.google.com/travel/flights?tfs=CHEAP");
        assertThat(cheapest.priceInsight().level()).isEqualTo("typical");
        assertThat(cheapest.priceInsight().typicalLow()).isEqualByComparingTo("610");
        assertThat(cheapest.priceInsight().typicalHigh()).isEqualByComparingTo("1050");

        FlightOffer direct = offers.get(1);
        assertThat(direct.stops()).isZero();
        assertThat(direct.stopovers()).isEmpty();
        assertThat(direct.departureTime()).isEqualTo(LocalTime.of(11, 40));
        assertThat(direct.priceInsight().level()).isEqualTo("high");

        server.verify();
    }

    @Test
    void un429SeTraduceEnCuotaAgotada() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> provider.searchOffers(
                "BCN", "NRT", LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 30), 1))
                .isInstanceOf(ProviderQuotaExceededException.class);
    }

    @Test
    void unaBusquedaDegradadaEsUnFalloTemporal() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Search-Status", "degraded");
        server.expect(requestTo(URL))
                .andRespond(withSuccess(SAMPLE_JSON, APPLICATION_JSON).headers(headers));

        // Temporary failure (the failover tries the next source), but NOT quota exceeded.
        assertThatThrownBy(() -> provider.searchOffers(
                "BCN", "NRT", LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 30), 1))
                .isInstanceOf(FlightProviderException.class)
                .isNotInstanceOf(ProviderQuotaExceededException.class);
    }

    @Test
    void sinResultadosDevuelveListaVacia() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Search-Status", "empty");
        server.expect(requestTo(URL)).andRespond(withSuccess("[]", APPLICATION_JSON).headers(headers));

        assertThat(provider.searchOffers(
                "BCN", "NRT", LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 30), 1)).isEmpty();
    }

    @Test
    void interpretaLasHorasDeGoogle() {
        assertThat(FlightPowersFlightProvider.parseTime("5:05 PM on Tue, Oct 6")).isEqualTo(LocalTime.of(17, 5));
        assertThat(FlightPowersFlightProvider.parseTime("12:30 AM on Wed, Oct 7")).isEqualTo(LocalTime.of(0, 30));
        assertThat(FlightPowersFlightProvider.parseTime("mañana")).isNull();
        assertThat(FlightPowersFlightProvider.parseTime(null)).isNull();
    }

    @Test
    void normalizaLasVariantesDelVeredicto() {
        assertThat(FlightPowersFlightProvider.normalizeLevel("Low")).isEqualTo("low");
        assertThat(FlightPowersFlightProvider.normalizeLevel(" typical ")).isEqualTo("typical");
        assertThat(FlightPowersFlightProvider.normalizeLevel("HIGH")).isEqualTo("high");
        assertThat(FlightPowersFlightProvider.normalizeLevel("cheaper than usual")).isEqualTo("low");
        assertThat(FlightPowersFlightProvider.normalizeLevel("")).isNull();
        assertThat(FlightPowersFlightProvider.normalizeLevel("unknown")).isNull();
    }
}
