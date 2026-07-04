package com.torii.history;

import com.torii.auth.SecurityConfig;
import com.torii.config.CorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP del histórico: parámetros normalizados y forma del JSON. */
@WebMvcTest(PriceHistoryController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class PriceHistoryControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PriceHistoryService service;

    @Test
    void devuelveLaSerieYNormalizaLosParametros() throws Exception {
        when(service.history("BCN", "NRT", 365)).thenReturn(List.of(
                new PricePointDto(LocalDate.of(2026, 7, 3), new BigDecimal("795.00"), "EUR")));

        // Códigos en minúscula y days fuera de rango: el controlador los normaliza.
        mvc.perform(get("/api/price-history")
                        .param("origin", "bcn")
                        .param("destination", "nrt")
                        .param("days", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-07-03"))
                .andExpect(jsonPath("$[0].price").value(795.00))
                .andExpect(jsonPath("$[0].currency").value("EUR"));

        verify(service).history("BCN", "NRT", 365); // 9999 → tope de 365
    }
}
