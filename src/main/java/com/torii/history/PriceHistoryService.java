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
 * Price history: best price seen per day and route.
 *
 * <p>It fills itself up: every search that returns real offers records (or improves)
 * that day's price. Just by using Torii normally the frontend chart gets populated
 * for free, no extra API calls.
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
     * Records the best offer of a search as TODAY's observation for the route. If
     * there's already one for today, it's only replaced when this one is cheaper.
     *
     * <p>Offers from {@link com.torii.provider.MockFlightProvider} are ignored, they're
     * made-up prices and would pollute the real history.
     */
    @Transactional
    public void recordObservation(String origin, String destination, List<FlightOffer> offers) {
        FlightOffer best = offers.stream()
                .filter(offer -> !isMockOffer(offer))
                .min(Comparator.comparing(FlightOffer::price))
                .orElse(null);
        if (best == null) {
            return; // no real offers, nothing to record
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

    /** Last {@code days} days of data for the frontend chart. */
    @Transactional(readOnly = true)
    public List<PricePointDto> history(String origin, String destination, int days) {
        LocalDate from = LocalDate.now(clock).minusDays(days - 1L);
        return repository
                .findByOriginAndDestinationAndDayGreaterThanEqualOrderByDayAsc(origin, destination, from)
                .stream()
                .map(PricePointDto::from)
                .toList();
    }

    /** Mock offers carry the example.com URL, they're not real prices. */
    private static boolean isMockOffer(FlightOffer offer) {
        return offer.bookingUrl() != null && offer.bookingUrl().contains("example.com");
    }

    /** Rough guess of the source based on the booking link. */
    private static String providerFromBookingUrl(String bookingUrl) {
        if (bookingUrl == null) return null;
        if (bookingUrl.contains("skyscanner")) return "FlightAPI";
        if (bookingUrl.contains("google.com")) return "GoogleFlights(SerpApi)";
        if (bookingUrl.contains("amadeus")) return "Amadeus";
        return null;
    }
}
