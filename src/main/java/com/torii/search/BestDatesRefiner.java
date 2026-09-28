package com.torii.search;

import com.torii.algorithm.SlidingWindowEngine;
import com.torii.algorithm.SlidingWindowEngine.SearchResult;
import com.torii.model.FlightOffer;
import com.torii.model.SearchRequest;
import com.torii.provider.FlightProvider;
import com.torii.provider.ProviderQuotaExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Second look at the cheapest dates of a search, with a source that sees more fares.
 *
 * <p>Why: FlightPowers (the main source, cheap enough to scan every date) only
 * returns Google Flights' highlighted results ("Mejores opciones"). Cheaper fares that
 * Google files under "Otros vuelos" never come back, e.g. BCN-NRT on 1-14 Dec 2026:
 * 729 EUR from FlightPowers against 575 EUR on Google. SerpApi does return those, but
 * its quota is too small to scan every date. So the scan stays on FlightPowers and,
 * once it's done, the {@code topDates} cheapest date pairs get asked again to SerpApi
 * and the results are merged.
 *
 * <p>It costs {@code topDates} calls per search, not per date, and it's cached like
 * any other lookup. It's best effort: if SerpApi fails or runs out of quota the
 * search answers with what FlightPowers found, and after a quota error it stops
 * trying for an hour. If FlightPowers ever returns every fare, switch this off with
 * {@code torii.search.refine.enabled=false}.
 */
public class BestDatesRefiner {

    private static final Logger log = LoggerFactory.getLogger(BestDatesRefiner.class);
    static final Duration QUOTA_PAUSE = Duration.ofHours(1);

    private final FlightProvider source;
    private final int topDates;
    private final Clock clock;
    private volatile Instant pausedUntil = Instant.MIN;

    public BestDatesRefiner(FlightProvider source, int topDates, Clock clock) {
        this.source = source;
        this.topDates = topDates;
        this.clock = clock;
    }

    /** Refiner that changes nothing, for when there's no second source configured. */
    public static BestDatesRefiner disabled() {
        return new BestDatesRefiner(null, 0, Clock.systemUTC());
    }

    public SearchResult refine(SearchRequest request, SearchResult result) {
        if (source == null || topDates <= 0 || clock.instant().isBefore(pausedUntil)) {
            return result;
        }
        List<FlightOffer> dates = result.cheapestPerDate().stream()
                .filter(o -> o.bookingUrl() == null || !o.bookingUrl().contains("example.com")) // mock prices
                .sorted(SlidingWindowEngine.BY_PRICE_THEN_STABLE)
                .limit(topDates)
                .toList();
        if (dates.isEmpty()) {
            return result;
        }

        List<FlightOffer> extra = fetch(request, dates);
        if (extra.isEmpty()) {
            return result;
        }

        List<FlightOffer> best = merge(result.offers(), extra.stream()
                .filter(o -> request.maxPrice() == null || o.price().compareTo(request.maxPrice()) <= 0)
                .toList(), request.topN());
        log.info("Refinado {}->{} con {}: {} fechas, mejor precio {} -> {}",
                request.origin(), request.destination(), source.name(), dates.size(),
                result.offers().isEmpty() ? "-" : result.offers().get(0).price(),
                best.isEmpty() ? "-" : best.get(0).price());
        return new SearchResult(best, cheapestPerDate(result.cheapestPerDate(), extra));
    }

    private List<FlightOffer> fetch(SearchRequest request, List<FlightOffer> dates) {
        List<FlightOffer> extra = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<FlightOffer>>> futures = dates.stream()
                    .map(d -> executor.submit(() -> source.searchOffers(request.origin(), request.destination(),
                            d.departDate(), d.returnDate(), request.maxStops())))
                    .toList();
            for (Future<List<FlightOffer>> future : futures) {
                try {
                    extra.addAll(future.get());
                } catch (java.util.concurrent.ExecutionException e) {
                    if (e.getCause() instanceof ProviderQuotaExceededException) {
                        pausedUntil = clock.instant().plus(QUOTA_PAUSE);
                        log.warn("Refinado pausado {}: {}", QUOTA_PAUSE, e.getCause().getMessage());
                    } else {
                        log.warn("Refinado: una fecha falló y se omite: {}", e.getCause().getMessage());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return extra;
    }

    /** Union of both lists without repeating the same flight, cheapest first, top N. */
    static List<FlightOffer> merge(List<FlightOffer> offers, List<FlightOffer> extra, int topN) {
        Map<List<Object>, FlightOffer> unique = new LinkedHashMap<>();
        for (FlightOffer o : offers) {
            unique.putIfAbsent(key(o), o);
        }
        for (FlightOffer o : extra) {
            unique.putIfAbsent(key(o), o);
        }
        return unique.values().stream()
                .sorted(SlidingWindowEngine.BY_PRICE_THEN_STABLE)
                .limit(topN)
                .toList();
    }

    /** Same airline, dates, departure time and price: the same flight from two sources. */
    private static List<Object> key(FlightOffer o) {
        return java.util.Arrays.asList(o.airline(), o.departDate(), o.returnDate(), o.departureTime(),
                o.price().stripTrailingZeros());
    }

    /** Keeps the cheapest offer per date pair, now counting the refined ones too. */
    private static List<FlightOffer> cheapestPerDate(List<FlightOffer> scanned, List<FlightOffer> extra) {
        Map<List<Object>, FlightOffer> byDate = new LinkedHashMap<>();
        for (FlightOffer o : scanned) {
            byDate.put(List.of(o.departDate(), o.returnDate()), o);
        }
        for (FlightOffer o : extra) {
            byDate.merge(List.of(o.departDate(), o.returnDate()), o,
                    (a, b) -> Comparator.comparing(FlightOffer::price).compare(a, b) <= 0 ? a : b);
        }
        return byDate.values().stream().filter(Objects::nonNull).toList();
    }
}
