package com.torii.algorithm;

import com.torii.model.FlightOffer;
import com.torii.model.SearchRequest;
import com.torii.provider.FlightProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * Core of Torii: a sliding window search over travel dates.
 *
 * <p>Given a holiday window (e.g. Jul 1 - Sep 30), a base trip length (e.g. 14 days)
 * and a variability (e.g. 3, so lengths 14 to 17), it goes through every combination
 * of departure date and length, asks a {@link FlightProvider} for prices and returns
 * the {@code topN} cheapest offers.
 *
 * <p>The lookups are I/O bound (mostly waiting on the network), so they run in
 * parallel on <b>virtual threads</b> (Java 21+). While one call is waiting, its
 * thread gets unmounted and the carrier thread picks up another one. A
 * {@link Semaphore} caps how many calls are in flight at once so we stay under the
 * rate limits of the external APIs. Total time goes from the <i>sum</i> of all calls
 * to roughly the time of a single batch.
 */
@Component
public class SlidingWindowEngine {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowEngine.class);

    /** One date pair to query (a single unit of work). */
    private record DatePair(LocalDate depart, LocalDate returnDate) {}

    /**
     * Deterministic final order: by price, then by departure date and airline to
     * break ties. Without the tie-breaker, the parallel run could return offers with
     * the same price in a different order on every search.
     */
    private static final Comparator<FlightOffer> BY_PRICE_THEN_STABLE =
            Comparator.comparing(FlightOffer::price)
                    .thenComparing(FlightOffer::departDate)
                    .thenComparing(FlightOffer::airline);

    private final FlightProvider provider;
    private final int maxConcurrency;

    public SlidingWindowEngine(FlightProvider provider,
                               @Value("${torii.search.max-concurrency:6}") int maxConcurrency) {
        this.provider = provider;
        this.maxConcurrency = maxConcurrency;
    }

    /**
     * Runs the search and returns the best offers sorted by price.
     *
     * @param request an already validated request
     * @return up to {@code request.topN()} offers, cheapest first
     */
    public List<FlightOffer> findBestOffers(SearchRequest request) {
        List<DatePair> pairs = buildDatePairs(request);
        List<FlightOffer> allCandidates = queryAllInParallel(request, pairs);

        long rangeDays = ChronoUnit.DAYS.between(request.rangeStart(), request.rangeEnd());
        log.info("Búsqueda {}->{} [{}]: {} días de rango, {} consultas (concurrencia {}), {} ofertas candidatas",
                request.origin(), request.destination(), request.precision(),
                rangeDays, pairs.size(), maxConcurrency, allCandidates.size());

        return allCandidates.stream()
                // Budget filter, if any. It's applied here on top of the cached data
                // so the same cache entries work for any budget.
                .filter(offer -> request.maxPrice() == null
                        || offer.price().compareTo(request.maxPrice()) <= 0)
                .sorted(BY_PRICE_THEN_STABLE)
                .limit(request.topN())
                .toList();
    }

    /**
     * Number of lookups (date pairs) this search will make. Used by the search
     * history and by the per-plan quota counter.
     */
    public int countQueries(SearchRequest request) {
        return buildDatePairs(request).size();
    }

    /** Builds every (outbound, return) pair to explore. Pure logic, no calls. */
    private List<DatePair> buildDatePairs(SearchRequest request) {
        // Precision sets how many days we move the departure date forward each step.
        // 1 (exhaustive) checks every day; 2 or 3 skip days to make fewer calls, at
        // the cost of possibly missing the exact cheapest day.
        int step = request.precision().dayStep();
        List<DatePair> pairs = new ArrayList<>();

        for (int duration = request.minDuration(); duration <= request.maxDuration(); duration++) {
            LocalDate lastValidDeparture = request.rangeEnd().minusDays(duration);
            for (LocalDate depart = request.rangeStart();
                 !depart.isAfter(lastValidDeparture);
                 depart = depart.plusDays(step)) {
                pairs.add(new DatePair(depart, depart.plusDays(duration)));
            }
        }
        return pairs;
    }

    /**
     * Queries every date pair in parallel on virtual threads, with concurrency
     * capped by the semaphore, and collects all the offers.
     */
    private List<FlightOffer> queryAllInParallel(SearchRequest request, List<DatePair> pairs) {
        Semaphore limit = new Semaphore(maxConcurrency);
        List<FlightOffer> all = new ArrayList<>();

        // try-with-resources: close() waits for all submitted tasks to finish.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<FlightOffer>>> futures = new ArrayList<>(pairs.size());

            for (DatePair pair : pairs) {
                futures.add(executor.submit(() -> {
                    limit.acquire(); // blocks if maxConcurrency calls are already in flight
                    try {
                        return provider.searchOffers(
                                request.origin(), request.destination(),
                                pair.depart(), pair.returnDate(), request.maxStops());
                    } finally {
                        limit.release();
                    }
                }));
            }

            for (Future<List<FlightOffer>> future : futures) {
                try {
                    all.addAll(future.get());
                } catch (ExecutionException e) {
                    // One date failed (e.g. no provider available). Don't kill the
                    // whole search over it, just carry on with the rest.
                    log.warn("Una consulta falló y se omite: {}", e.getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return all;
    }
}
