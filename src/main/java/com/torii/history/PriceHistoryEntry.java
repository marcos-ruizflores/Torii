package com.torii.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A row in {@code price_history}: the best price seen for a route on a given day.
 *
 * <p>JPA entities are mutable classes with a no-arg constructor (Hibernate requires
 * it), unlike our domain records. That's why this class isn't exposed through the
 * API, the controller returns {@link PricePointDto} instead.
 */
@Entity
@Table(name = "price_history")
public class PriceHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3)
    private String origin;

    @Column(nullable = false, length = 3)
    private String destination;

    // The column is observed_on because "day" is a reserved word in H2. In Java it
    // stays "day" since that's what the rest of the code uses.
    @Column(name = "observed_on", nullable = false)
    private LocalDate day;

    @Column(name = "best_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal bestPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 60)
    private String provider;

    /** No-arg constructor required by JPA, don't use directly. */
    protected PriceHistoryEntry() {}

    public PriceHistoryEntry(String origin, String destination, LocalDate day,
                             BigDecimal bestPrice, String currency, String provider) {
        this.origin = origin;
        this.destination = destination;
        this.day = day;
        this.bestPrice = bestPrice;
        this.currency = currency;
        this.provider = provider;
    }

    /** Replaces the day's price if the new observation is cheaper. */
    public void updateIfCheaper(BigDecimal newPrice, String newCurrency, String newProvider) {
        if (newPrice.compareTo(bestPrice) < 0) {
            this.bestPrice = newPrice;
            this.currency = newCurrency;
            this.provider = newProvider;
        }
    }

    public Long getId() {
        return id;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDate getDay() {
        return day;
    }

    public BigDecimal getBestPrice() {
        return bestPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProvider() {
        return provider;
    }
}
