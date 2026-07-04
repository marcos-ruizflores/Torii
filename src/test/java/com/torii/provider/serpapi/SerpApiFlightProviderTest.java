package com.torii.provider.serpapi;

import com.torii.config.SerpApiProperties;
import com.torii.model.FlightOffer;
import com.torii.provider.ProviderQuotaExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Test del proveedor SerpApi (Google Flights) contra un servidor HTTP simulado.
 * Verifica el mapeo de best_flights + other_flights a {@link FlightOffer} y que un
 * 429 se traduce en cuota agotada para que el failover lo aparque.
 */
class SerpApiFlightProviderTest {

    private MockRestServiceServer server;
    private SerpApiFlightProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new SerpApiFlightProvider(builder, SerpApiProperties.defaults());
    }

    @Test
    void mapeaBestYOtherFlightsAFlightOffers() {
        String json = """
                {
                  "best_flights": [
                    { "flights": [ {"airline":"Iberia",
                        "departure_airport": {"id":"BCN","time":"2026-07-01 10:45"}} ],
                      "layovers": [], "price": 520 }
                  ],
                  "other_flights": [
                    { "flights": [ {"airline":"Lufthansa",
                        "departure_airport": {"id":"BCN","time":"2026-07-01 06:30"}},
                        {"airline":"Lufthansa"} ],
                      "layovers": [ {"id":"FRA","name":"Frankfurt"} ], "price": 480 }
                  ]
                }
                """;

        server.expect(requestTo(containsString("/search")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("engine", "google_flights"))
                .andExpect(queryParam("departure_id", "BCN"))
                .andExpect(queryParam("type", "1")) // round trip
                .andRespond(withSuccess(json, APPLICATION_JSON));

        List<FlightOffer> offers = provider.searchOffers(
                "BCN", "NRT", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15), 1);

        assertThat(offers).hasSize(2);

        FlightOffer best = offers.get(0);
        assertThat(best.airline()).isEqualTo("Iberia");
        assertThat(best.price()).isEqualByComparingTo("520");
        assertThat(best.stops()).isZero();
        assertThat(best.departDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(best.returnDate()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(best.departureTime()).isEqualTo(LocalTime.of(10, 45));
        assertThat(best.stopovers()).isEmpty();

        FlightOffer other = offers.get(1);
        assertThat(other.airline()).isEqualTo("Lufthansa");
        assertThat(other.price()).isEqualByComparingTo("480");
        assertThat(other.stops()).isEqualTo(1); // una escala (un layover)
        assertThat(other.departureTime()).isEqualTo(LocalTime.of(6, 30));
        assertThat(other.stopovers()).containsExactly("FRA");

        server.verify();
    }

    @Test
    void traduceUn429AProviderQuotaExceeded() {
        server.expect(requestTo(containsString("/search")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> provider.searchOffers(
                "BCN", "NRT", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15), 1))
                .isInstanceOf(ProviderQuotaExceededException.class);
    }
}
