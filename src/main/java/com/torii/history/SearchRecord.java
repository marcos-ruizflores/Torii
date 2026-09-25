package com.torii.history;

import com.torii.model.SearchRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A stored search ({@code searches} table): what the user asked for and how many
 * lookups it cost. {@code userId} is null for anonymous searches (not logged in).
 */
@Entity
@Table(name = "searches")
public class SearchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 3)
    private String origin;

    @Column(nullable = false, length = 3)
    private String destination;

    @Column(name = "range_start", nullable = false)
    private LocalDate rangeStart;

    @Column(name = "range_end", nullable = false)
    private LocalDate rangeEnd;

    @Column(name = "base_duration", nullable = false)
    private int baseDuration;

    @Column(nullable = false)
    private int variability;

    @Column(name = "search_precision", nullable = false, length = 20)
    private String precision;

    @Column(name = "max_stops", nullable = false)
    private int maxStops;

    @Column(name = "top_n", nullable = false)
    private int topN;

    @Column(name = "max_price", precision = 10, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "queries_used", nullable = false)
    private int queriesUsed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** No-arg constructor required by JPA, don't use directly. */
    protected SearchRecord() {}

    public SearchRecord(Long userId, SearchRequest request, int queriesUsed) {
        this.userId = userId;
        this.origin = request.origin();
        this.destination = request.destination();
        this.rangeStart = request.rangeStart();
        this.rangeEnd = request.rangeEnd();
        this.baseDuration = request.baseDuration();
        this.variability = request.variability();
        this.precision = request.precision().name();
        this.maxStops = request.maxStops();
        this.topN = request.topN();
        this.maxPrice = request.maxPrice();
        this.queriesUsed = queriesUsed;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDate getRangeStart() {
        return rangeStart;
    }

    public LocalDate getRangeEnd() {
        return rangeEnd;
    }

    public int getBaseDuration() {
        return baseDuration;
    }

    public int getVariability() {
        return variability;
    }

    public String getPrecision() {
        return precision;
    }

    public int getMaxStops() {
        return maxStops;
    }

    public int getTopN() {
        return topN;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public int getQueriesUsed() {
        return queriesUsed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
