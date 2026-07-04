package com.torii.provider.amadeus;

import com.torii.config.AmadeusProperties;
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
 * Test del proveedor de Amadeus contra un servidor HTTP simulado. Verifica las dos
 * cosas con lógica: que mapea bien la respuesta a {@link FlightOffer}, y que un 429
 * se convierte en {@link ProviderQuotaExceededException} para que el failover lo
 * aparque.
 */
class AmadeusFlightProviderTest {

    private MockRestServiceServer server;
    private AmadeusFlightProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        // Token fijo (no probamos aquí la autenticación, eso es del AuthClientTest).
        provider = new AmadeusFlightProvider(builder, () -> "TEST_TOKEN", AmadeusProperties.defaults());
    }

    @Test
    void mapeaLaRespuestaDeAmadeusAFlightOffer() {
        // Respuesta recortada con la forma real de Amadeus: ida (BCN→NRT) y vuelta (NRT→BCN).
        String json = """
                {
                  "data": [
                    {
                      "price": { "grandTotal": "325.50", "currency": "EUR" },
                      "validatingAirlineCodes": ["IB"],
                      "itineraries": [
                        { "segments": [
                            { "departure": {"iataCode":"BCN","at":"2026-07-01T10:30:00"},
                              "arrival":   {"iataCode":"NRT","at":"2026-07-02T08:00:00"} }
                        ]},
                        { "segments": [
                            { "departure": {"iataCode":"NRT","at":"2026-07-15T11:00:00"},
                              "arrival":   {"iataCode":"BCN","at":"2026-07-15T20:00:00"} }
                        ]}
                      ]
                    }
                  ]
                }
                """;

        server.expect(requestTo(containsString("/v2/shopping/flight-offers")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("originLocationCode", "BCN"))
                .andExpect(queryParam("destinationLocationCode", "NRT"))
                .andRespond(withSuccess(json, APPLICATION_JSON));

        List<FlightOffer> offers = provider.searchOffers(
                "BCN", "NRT", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15), 1);

        assertThat(offers).hasSize(1);
        FlightOffer o = offers.get(0);
        assertThat(o.airline()).isEqualTo("IB");
        assertThat(o.price()).isEqualByComparingTo("325.50");
        assertThat(o.currency()).isEqualTo("EUR");
        assertThat(o.stops()).isZero(); // un segmento por trayecto → directo
        assertThat(o.departDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(o.returnDate()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(o.departureTime()).isEqualTo(LocalTime.of(10, 30)); // de "2026-07-01T10:30:00"
        assertThat(o.stopovers()).isEmpty(); // ida con un solo segmento → sin escalas
        server.verify();
    }

    @Test
    void traduceUn429AProviderQuotaExceeded() {
        server.expect(requestTo(containsString("/v2/shopping/flight-offers")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> provider.searchOffers(
                "BCN", "NRT", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15), 1))
                .isInstanceOf(ProviderQuotaExceededException.class);
    }
}
