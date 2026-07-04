package com.torii.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Consultas consumidas por un usuario en un mes (tabla {@code plan_usage}).
 * La clave es compuesta (usuario + mes): una fila por usuario y mes; el mes se
 * normaliza siempre a su primer día.
 */
@Entity
@Table(name = "plan_usage")
public class PlanUsage {

    /** Clave compuesta (user_id, usage_month) — JPA exige una clase aparte para esto. */
    @Embeddable
    public record Key(
            @Column(name = "user_id") Long userId,
            @Column(name = "usage_month") LocalDate usageMonth
    ) implements Serializable {}

    @EmbeddedId
    private Key key;

    @Column(name = "queries_used", nullable = false)
    private int queriesUsed;

    /** Constructor vacío exigido por JPA; no usar directamente. */
    protected PlanUsage() {}

    public PlanUsage(Long userId, LocalDate month) {
        this.key = new Key(userId, month.withDayOfMonth(1));
        this.queriesUsed = 0;
    }

    public void add(int queries) {
        this.queriesUsed += queries;
    }

    public Key getKey() {
        return key;
    }

    public int getQueriesUsed() {
        return queriesUsed;
    }
}
