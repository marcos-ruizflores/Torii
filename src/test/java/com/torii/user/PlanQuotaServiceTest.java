package com.torii.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de la cuota mensual por plan: FREE tiene 30 consultas/mes, gastar dentro
 * del límite acumula, pasarse rechaza con 429 sin descontar nada.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PlanQuotaService.class)
class PlanQuotaServiceTest {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-07-04T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private PlanQuotaService quota;

    @Autowired
    private UserRepository users;

    private Long userId;

    @BeforeEach
    void creaUsuarioFree() {
        userId = users.save(new UserAccount("free@test.com", "hash", "Free")).getId();
    }

    @Test
    void acumulaConsultasDentroDelLimite() {
        quota.consume(userId, 10);
        quota.consume(userId, 15);

        PlanQuotaService.Usage usage = quota.usageOf(userId);
        assertThat(usage.used()).isEqualTo(25);
        assertThat(usage.limit()).isEqualTo(30); // plan FREE
        assertThat(usage.plan()).isEqualTo("FREE");
    }

    @Test
    void rechazaCon429SiNoQuedaCuotaYNoDescuentaNada() {
        quota.consume(userId, 25);

        assertThatThrownBy(() -> quota.consume(userId, 10)) // 25 + 10 > 30
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // El intento fallido no debe haber consumido nada.
        assertThat(quota.usageOf(userId).used()).isEqualTo(25);
    }

    @Test
    void justoElLimiteExactoSePermite() {
        quota.consume(userId, 30);
        assertThat(quota.usageOf(userId).used()).isEqualTo(30);
    }
}
