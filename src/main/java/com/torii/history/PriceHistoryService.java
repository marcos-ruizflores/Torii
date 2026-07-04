package com.torii.history;

import com.torii.model.FlightOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Histórico de precios: el mejor precio observado por día y ruta.
 *
 * <p>Se alimenta solo: cada búsqueda que devuelve ofertas reales registra (o
 * mejora) el precio del día. Con el uso normal de Torii, el gráfico del frontend
 * se va llenando gratis, sin llamadas extra a las APIs.
 */
@Service
public class PriceHistoryService {

    private static final Logger log = LoggerFactory.getLogger(PriceHistoryService.class);

    private final PriceHistoryRepository repository;
    private final Clock clock;

    public PriceHistoryService(PriceHistoryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Registra la mejor oferta de una búsqueda como observación de HOY para la ruta.
     * Si ya había una observación hoy, solo la sustituye si esta es más barata.
     *
     * <p>Las ofertas del {@link com.torii.provider.MockFlightProvider} se ignoran:
     * son precios inventados y contaminarían el histórico real.
     */
    @Transactional
    public void recordObservation(String origin, String destination, List<FlightOffer> offers) {
        FlightOffer best = offers.stream()
                .filter(offer -> !isMockOffer(offer))
                .min(Comparator.comparing(FlightOffer::price))
                .orElse(null);
        if (best == null) {
            return; // sin ofertas reales, no hay nada que registrar
        }

        LocalDate today = LocalDate.now(clock);
        String provider = providerFromBookingUrl(best.bookingUrl());

        repository.findByOriginAndDestinationAndDay(origin, destination, today)
                .ifPresentOrElse(
                        existing -> existing.updateIfCheaper(best.price(), best.currency(), provider),
                        () -> repository.save(new PriceHistoryEntry(
                                origin, destination, today, best.price(), best.currency(), provider)));

        log.debug("Histórico {}->{}: observado {} {} el {}",
                origin, destination, best.price(), best.currency(), today);
    }

    /** Serie de los últimos {@code days} días para el gráfico del frontend. */
    @Transactional(readOnly = true)
    public List<PricePointDto> history(String origin, String destination, int days) {
        LocalDate from = LocalDate.now(clock).minusDays(days - 1L);
        return repository
                .findByOriginAndDestinationAndDayGreaterThanEqualOrderByDayAsc(origin, destination, from)
                .stream()
                .map(PricePointDto::from)
                .toList();
    }

    /** Las ofertas del mock llevan su URL de ejemplo: no son precios de verdad. */
    private static boolean isMockOffer(FlightOffer offer) {
        return offer.bookingUrl() != null && offer.bookingUrl().contains("example.com");
    }

    /** Deducción simple de la fuente a partir del enlace de reserva. */
    private static String providerFromBookingUrl(String bookingUrl) {
        if (bookingUrl == null) return null;
        if (bookingUrl.contains("skyscanner")) return "FlightAPI";
        if (bookingUrl.contains("google.com")) return "GoogleFlights(SerpApi)";
        if (bookingUrl.contains("amadeus")) return "Amadeus";
        return null;
    }
}
