package com.torii.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.transaction.TestTransaction;
import com.torii.email.PasswordResetRequestedEvent;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the full auth flow with the whole app running on H2:
 * sign up -> search WITH token -> "my searches" contains it. Also checks the two
 * security contracts: /api/me without a token is 401 and anonymous search still works.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // each test leaves the DB as it found it
@RecordApplicationEvents
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationEvents events;

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
        // 1. Sign up -> token.
        String authJson = mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Marcos","email":"marcos@test.com","password":"superclave123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.plan").value("FREE"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(authJson, "$.token");

        // 2. Search WITH the token (the engine uses MockFlightProvider, no network).
        mvc.perform(post("/api/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchBody()))
                .andExpect(status().isOk());

        // The search gets recorded in a separate logical transaction, so force it
        // to be visible inside the test.
        TestTransaction.flagForCommit();

        // 3. The search shows up in "my searches".
        mvc.perform(get("/api/me/searches").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].origin").value("BCN"))
                .andExpect(jsonPath("$[0].destination").value("MAD"))
                .andExpect(jsonPath("$[0].precision").value("FAST")); // no precision sent: FREE runs at Fast

        // 4. The token's profile endpoint answers.
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("marcos@test.com"));
    }

    @Test
    void cambiarDePlanEstaDesactivadoHastaQueHayaPagos() throws Exception {
        String authJson = mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Marcos","email":"plan@test.com","password":"superclave123"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(authJson, "$.token");

        // New accounts start on FREE (limit 30)...
        mvc.perform(get("/api/me/usage").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.plan").value("FREE"))
                .andExpect(jsonPath("$.limit").value(30));

        // ...trying to upgrade is refused while there are no payments...
        mvc.perform(post("/api/me/plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"PRO\"}"))
                .andExpect(status().isForbidden());

        // ...and the account is still on FREE.
        mvc.perform(get("/api/me/usage").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.plan").value("FREE"))
                .andExpect(jsonPath("$.limit").value(30));
    }

    @Test
    void sinTokenTantoMisBusquedasComoBuscarSon401() throws Exception {
        // Without an account you can't see the history...
        mvc.perform(get("/api/me/searches")).andExpect(status().isUnauthorized());

        // ...or search: searching uses quota and paid calls, so it needs a token.
        mvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restablecerLaPasswordCierraLasSesionesAnteriores() throws Exception {
        String oldToken = JsonPath.read(mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Marcos","email":"reset@test.com","password":"superclave123"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.token");

        // Same answer for known and unknown emails.
        mvc.perform(post("/api/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reset@test.com\"}"))
                .andExpect(status().isAccepted());
        mvc.perform(post("/api/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@test.com\"}"))
                .andExpect(status().isAccepted());
        String resetToken = events.stream(PasswordResetRequestedEvent.class).findFirst().orElseThrow().token();

        String newToken = JsonPath.read(mvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"password\":\"clavenueva456\"}".formatted(resetToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.emailVerified").value(true))
                .andReturn().getResponse().getContentAsString(), "$.token");

        // The session opened before the reset is closed, the new one works.
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());

        // The link is single use.
        mvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"password\":\"otraclave789\"}".formatted(resetToken)))
                .andExpect(status().isBadRequest());
    }
}
