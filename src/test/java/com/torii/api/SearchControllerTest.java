package com.torii.api;

import com.torii.auth.SecurityConfig;
import com.torii.config.CorsConfig;
import com.torii.model.FlightOffer;
import com.torii.search.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests for search. Loads ONLY the controller (plus validation, the
 * ApiExceptionHandler and JSON serialization), with {@link SearchService} mocked.
 * That covers the HTTP contract (deserialization, validation, status codes, JSON
 * shape) without starting the whole app or running the algorithm.
 */
@WebMvcTest(SearchController.class)
// Real security chain: POST /api/search needs a token (searching uses quota) and
// CORS is handled by the Security filter using the CorsConfig bean.
@Import({SecurityConfig.class, CorsConfig.class})
class SearchControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SearchService searchService;

    /**
     * Fakes an authenticated request without signing a real token: injects a dummy
     * Jwt with subject "1" (the controller does Long.valueOf(subject)).
     */
    private static RequestPostProcessor asUser() {
        return jwt().jwt(token -> token.subject("1"));
    }

    // Dates relative to "today" so the test doesn't expire over time (with fixed
    // dates the "must be in the future" check ended up rejecting them).
    private static final LocalDate RANGE_START = LocalDate.now().plusMonths(1);
    private static final LocalDate RANGE_END = LocalDate.now().plusMonths(4);

    private static final String VALID_BODY = """
            {
              "origin": "BCN",
              "destination": "NRT",
              "rangeStart": "%s",
              "rangeEnd": "%s",
              "baseDuration": 14,
              "variability": 3,
              "maxStops": 1,
              "topN": 5
            }
            """.formatted(RANGE_START, RANGE_END);

    @Test
    void busquedaValidaDevuelve200YElJsonDeOfertas() throws Exception {
        when(searchService.search(any(), any())).thenReturn(List.of(
                new FlightOffer("Iberia", new BigDecimal("325.00"), "EUR", 1,
                        LocalDate.of(2026, 7, 29), LocalDate.of(2026, 8, 12),
                        "https://example.com/booking")));

        mvc.perform(post("/api/search")
                        .with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].airline").value("Iberia"))
                .andExpect(jsonPath("$[0].currency").value("EUR"))
                .andExpect(jsonPath("$[0].stops").value(1))
                .andExpect(jsonPath("$[0].departDate").value("2026-07-29"))
                .andExpect(jsonPath("$[0].bookingUrl").value("https://example.com/booking"));
    }

    @Test
    void codigoIataInvalidoDevuelve400() throws Exception {
        String body = VALID_BODY.replace("\"NRT\"", "\"TOKYO\"");

        mvc.perform(post("/api/search")
                        .with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("IATA")));
    }

    @Test
    void faltaUnCampoObligatorioDevuelve400() throws Exception {
        String body = """
                {
                  "destination": "NRT",
                  "rangeStart": "%s",
                  "rangeEnd": "%s",
                  "baseDuration": 14,
                  "variability": 3,
                  "maxStops": 1,
                  "topN": 5
                }
                """.formatted(RANGE_START, RANGE_END); // "origin" is missing

        mvc.perform(post("/api/search")
                        .with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("origin")));
    }

    @Test
    void rangoMasCortoQueLaEstanciaDevuelve400() throws Exception {
        // 9 day range but stays up to 17 days -> fails the cross check in toDomain().
        String body = VALID_BODY.replace(RANGE_END.toString(), RANGE_START.plusDays(9).toString());

        mvc.perform(post("/api/search")
                        .with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("demasiado corto")));
    }

    // --- CORS (frontend deployed on another origin, see CorsConfig) ---

    @Test
    void preflightDesdeOrigenPermitidoDevuelveCabecerasCors() throws Exception {
        // The browser sends this OPTIONS before the real POST when the frontend lives
        // on another origin. Without the Allow-Origin header it would block the call.
        mvc.perform(options("/api/search")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void preflightDesdeOrigenDesconocidoSeRechaza() throws Exception {
        mvc.perform(options("/api/search")
                        .header("Origin", "https://malicioso.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void precisionInvalidaDevuelve400() throws Exception {
        String body = VALID_BODY.replace("\"topN\": 5", "\"topN\": 5,\n  \"precision\": \"TURBO\"");

        mvc.perform(post("/api/search")
                        .with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("precision no válida")));
    }

    // --- Getaway filter (departDayOfWeek / returnDayOfWeek) ---

    @Test
    void filtroDeFinDeSemanaValidoDevuelve200() throws Exception {
        when(searchService.search(any(), any())).thenReturn(List.of());
        String body = VALID_BODY.replace("\"topN\": 5",
                "\"topN\": 5,\n  \"departDayOfWeek\": \"FRIDAY\",\n  \"returnDayOfWeek\": \"SUNDAY\"");

        mvc.perform(post("/api/search")
                        .with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void filtroDeEscapadasConSoloUnDiaDevuelve400() throws Exception {
        String body = VALID_BODY.replace("\"topN\": 5",
                "\"topN\": 5,\n  \"departDayOfWeek\": \"FRIDAY\"");

        mvc.perform(post("/api/search")
                        .with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("juntos")));
    }

    @Test
    void diaDeLaSemanaInvalidoDevuelve400() throws Exception {
        String body = VALID_BODY.replace("\"topN\": 5",
                "\"topN\": 5,\n  \"departDayOfWeek\": \"VIERNES\",\n  \"returnDayOfWeek\": \"SUNDAY\"");

        mvc.perform(post("/api/search")
                        .with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("departDayOfWeek no válido")));
    }

    @Test
    void conFiltroDeEscapadasLaDuracionLargaYaNoInvalidaElRango() throws Exception {
        when(searchService.search(any(), any())).thenReturn(List.of());
        // 9 day range: without the filter it would fail as "too short" (14-17 day stay),
        // but with Friday to Sunday the pattern sets the length and it fits.
        String body = VALID_BODY
                .replace(RANGE_END.toString(), RANGE_START.plusDays(9).toString())
                .replace("\"topN\": 5",
                        "\"topN\": 5,\n  \"departDayOfWeek\": \"FRIDAY\",\n  \"returnDayOfWeek\": \"SUNDAY\"");

        mvc.perform(post("/api/search")
                        .with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void busquedaSinTokenDevuelve401() throws Exception {
        // Without .with(asUser()) the request is anonymous: the security chain stops it
        // with a 401 BEFORE it reaches the controller (the body isn't even validated).
        mvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }
}
