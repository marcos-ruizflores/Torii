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
 * Una fila de la tabla {@code price_history}: el mejor precio observado un día
 * concreto para una ruta.
 *
 * <p>Nota de diseño: las entidades JPA son clases mutables con constructor vacío
 * (lo exige Hibernate), a diferencia de nuestros records de dominio. Por eso esta
 * clase vive en su propio paquete y NO se expone por la API: el controlador
 * devuelve {@link PricePointDto}.
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

    // En BD se llama observed_on ("day" es palabra reservada en H2); en Java
    // seguimos hablando de "day" porque es como lo consume el resto del código.
    @Column(name = "observed_on", nullable = false)
    private LocalDate day;

    @Column(name = "best_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal bestPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 60)
    private String provider;

    /** Constructor vacío exigido por JPA; no usar directamente. */
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

    /** Sustituye el precio del día si la nueva observación es más barata. */
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
