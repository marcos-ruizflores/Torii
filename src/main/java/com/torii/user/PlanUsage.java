package com.torii.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Lookups used by a user in a given month ({@code plan_usage} table). Composite key
 * (user + month), so one row per user per month. The month is always normalized to
 * its first day.
 */
@Entity
@Table(name = "plan_usage")
public class PlanUsage {

    /** Composite key (user_id, usage_month). JPA needs a separate class for it. */
    @Embeddable
    public record Key(
            @Column(name = "user_id") Long userId,
            @Column(name = "usage_month") LocalDate usageMonth
    ) implements Serializable {}

    @EmbeddedId
    private Key key;

    @Column(name = "queries_used", nullable = false)
    private int queriesUsed;

    /** No-arg constructor required by JPA, don't use directly. */
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
