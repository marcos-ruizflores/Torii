package com.torii.provider.flightapi;

import com.torii.config.FlightApiProperties;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * FlightAPI.io provider test against a mock HTTP server. Checks that the normalized
 * model (itineraries -> legs -> carriers/places by ID) resolves correctly to
 * {@link FlightOffer}, results come sorted by price, maxStops is filtered client side
 * and a 429 becomes quota exceeded for the failover.
 */
class FlightApiFlightProviderTest {

    /**
     * Response with 2 itineraries: a direct Iberia one (640 EUR) and a cheaper Qatar
     * one via DOH (520.5 EUR). They're OUT OF ORDER on purpose to check the provider
     * sorts by price.
     */
    private static final String SAMPLE_JSON = """
            {
              "itineraries": [
                {
                  "id": "IT-IBERIA",
                  "leg_ids": ["LEG-OUT-IB", "LEG-BACK-IB"],
                  "pricing_options": [
                    { "price": {"amount": 640.0},
                      "items": [ {"url": "/transport_deeplink/4.0/ES/es-ES/EUR/iber/1/flights",
                                  "price": {"amount": 640.0}} ] }
                  ],
                  "cheapest_price": {"amount": 640.0}
                },
                {
                  "id": "IT-QATAR",
                  "leg_ids": ["LEG-OUT-QR", "LEG-BACK-QR"],
                  "pricing_options": [
                    { "price": {"amount": 520.5},
                      "items": [ {"url": "/transport_deeplink/4.0/ES/es-ES/EUR/qata/1/flights",
                                  "price": {"amount": 520.5}} ] }
                  ],
                  "cheapest_price": {"amount": 520.5}
                }
              ],
              "legs": [
                { "id": "LEG-OUT-IB", "departure": "2026-07-01T10:45:00",
                  "stop_count": 0, "marketing_carrier_ids": [-31], "stop_ids": [] },
                { "id": "LEG-BACK-IB", "departure": "2026-07-15T18:00:00",
                  "stop_count": 0, "marketing_carrier_ids": [-31], "stop_ids": [] },
                { "id": "LEG-OUT-QR", "departure": "2026-07-01T06:30:00",
                  "stop_count": 1, "marketing_carrier_ids": [-32], "stop_ids": [[9596]] },
                { "id": "LEG-BACK-QR", "departure": "2026-07-15T21:10:00",
                  "stop_count": 1, "marketing_carrier_ids": [-32], "stop_ids": [[9596]] }
              ],
              "carriers": [
                { "id": -31, "name": "Iberia", "display_code": "IB" },
                { "id": -32, "name": "Qatar Airways", "display_code": "QR" }
              ],
              "places": [
                { "id": 9596, "display_code": "DOH", "name": "Hamad International Airport" }
              ]
            }
            """;

    private MockRestServiceServer server;
    private FlightApiFlightProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new FlightApiFlightProvider(builder, FlightApiProperties.defaults().withApiKey("TESTKEY"));
    }

    @Test
    void resuelveElModeloNormalizadoYOrdenaPorPrecio() {
        server.expect(requestTo(containsString(
                        "/roundtrip/TESTKEY/BCN/NRT/2026-07-01/2026-07-15/1/0/0/Economy/EUR")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(SAMPLE_JSON, APPLICATION_JSON));

        List<FlightOffer> offers = provider.searchOffers(
                "BCN", "NRT", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15), 2);

        assertThat(offers).hasSize(2);

        // Cheapest first, even though it was second in the JSON.
        FlightOffer cheapest = offers.get(0);
        assertThat(cheapest.airline()).isEqualTo("Qatar Airways"); // carrier -32 resolved
        assertThat(cheapest.price()).isEqualByComparingTo("520.5");
        assertThat(cheapest.stops()).isEqualTo(1);
        assertThat(cheapest.stopovers()).containsExactly("DOH"); // place 9596 resolved
        assertThat(cheapest.departureTime()).isEqualTo(LocalTime.of(6, 30));
        assertThat(cheapest.returnDepartureTime()).isEqualTo(LocalTime.of(21, 10)); // LEG-BACK-QR
        assertThat(cheapest.departDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(cheapest.returnDate()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(cheapest.bookingUrl())
                .startsWith("https://www.skyscanner.net/transport_deeplink/");

        FlightOffer direct = offers.get(1);
        assertThat(direct.airline()).isEqualTo("Iberia");
        assertThat(direct.price()).isEqualByComparingTo("640");
        assertThat(direct.stops()).isZero();
        assertThat(direct.stopovers()).isEmpty();
        assertThat(direct.departureTime()).isEqualTo(LocalTime.of(10, 45));
        assertThat(direct.returnDepartureTime()).isEqualTo(LocalTime.of(18, 0)); // LEG-BACK-IB

        server.verify();
    }

    @Test
    void filtraEnClienteLasOfertasConDemasiadasEscalas() {
        server.expect(requestTo(containsString("/roundtrip/TESTKEY/BCN/NRT")))
                .andRespond(withSuccess(SAMPLE_JSON, APPLICATION_JSON));

        // maxStops=0: the Qatar offer (1 stop) has to be filtered out.
        List<FlightOffer> offers = provider.searchOffers(
                "BCN", "NRT", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15), 0);

        assertThat(offers).hasSize(1);
        assertThat(offers.get(0).airline()).isEqualTo("Iberia");
    }

    @Test
    void traduceUn429AProviderQuotaExceeded() {
        server.expect(requestTo(containsString("/roundtrip/")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> provider.searchOffers(
                "BCN", "NRT", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15), 1))
                .isInstanceOf(ProviderQuotaExceededException.class);
    }
}
