package com.torii.api;

import com.torii.model.FlightOffer;
import com.torii.search.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la capa web del buscador. Cargan SOLO el controlador (más la validación,
 * el ApiExceptionHandler y la serialización JSON); el {@link SearchService} se
 * sustituye por un mock. Así se prueba el "contrato HTTP" — deserialización,
 * validación, códigos de estado y forma del JSON — sin levantar la app entera ni
 * ejecutar el algoritmo.
 */
@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SearchService searchService;

    // Fechas relativas a "hoy" para que el test no caduque con el paso del tiempo
    // (con fechas fijas, la validación "debe ser futura" acababa rechazándolas).
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
        when(searchService.search(any())).thenReturn(List.of(
                new FlightOffer("Iberia", new BigDecimal("325.00"), "EUR", 1,
                        LocalDate.of(2026, 7, 29), LocalDate.of(2026, 8, 12),
                        "https://example.com/booking")));

        mvc.perform(post("/api/search")
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
                """.formatted(RANGE_START, RANGE_END); // falta "origin"

        mvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("origin")));
    }

    @Test
    void rangoMasCortoQueLaEstanciaDevuelve400() throws Exception {
        // 9 días de rango pero estancia de hasta 17 → falla la validación cruzada de toDomain().
        String body = VALID_BODY.replace(RANGE_END.toString(), RANGE_START.plusDays(9).toString());

        mvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("demasiado corto")));
    }

    // --- CORS (frontend desplegado en otro origen; ver CorsConfig) ---

    @Test
    void preflightDesdeOrigenPermitidoDevuelveCabecerasCors() throws Exception {
        // El navegador manda este OPTIONS antes del POST real cuando el frontend
        // vive en otro origen. Sin la cabecera Allow-Origin, bloquearía la llamada.
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("precision no válida")));
    }
}
