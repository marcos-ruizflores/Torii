package com.torii.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integración del flujo completo de autenticación, con la app entera
 * levantada sobre H2: registro → búsqueda CON token → "mis búsquedas" la contiene.
 * También verifica los dos contratos de seguridad: /api/me sin token es 401 y la
 * búsqueda anónima sigue funcionando.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // cada test deja la BD como estaba
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private String searchBody() {
        LocalDate start = LocalDate.now().plusMonths(2);
        return """
                {
                  "origin": "BCN", "destination": "MAD",
                  "rangeStart": "%s", "rangeEnd": "%s",
                  "baseDuration": 14, "variability": 0, "maxStops": 2, "topN": 3
                }
                """.formatted(start, start.plusDays(15));
    }

    @Test
    void registroBusquedaConTokenYMisBusquedas() throws Exception {
        // 1. Registro → token.
        String authJson = mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Marcos","email":"marcos@test.com","password":"superclave123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.plan").value("FREE"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(authJson, "$.token");

        // 2. Búsqueda CON el token (el motor usa el MockFlightProvider: sin red).
        mvc.perform(post("/api/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchBody()))
                .andExpect(status().isOk());

        // El registro de la búsqueda ocurre en otra transacción lógica; forzamos
        // visibilidad dentro del test.
        TestTransaction.flagForCommit();

        // 3. La búsqueda aparece en "mis búsquedas".
        mvc.perform(get("/api/me/searches").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].origin").value("BCN"))
                .andExpect(jsonPath("$[0].destination").value("MAD"))
                .andExpect(jsonPath("$[0].precision").value("EXHAUSTIVE"));

        // 4. El perfil del token responde.
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("marcos@test.com"));
    }

    @Test
    void sinTokenMisBusquedasEs401PeroBuscarSigueSiendoPublico() throws Exception {
        mvc.perform(get("/api/me/searches")).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchBody()))
                .andExpect(status().isOk());
    }
}
