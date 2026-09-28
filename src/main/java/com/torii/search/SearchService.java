package com.torii.search;

import com.torii.algorithm.SlidingWindowEngine;
import com.torii.history.PriceHistoryService;
import com.torii.history.PriceVerdictService;
import com.torii.history.SearchHistoryService;
import com.torii.model.FlightOffer;
import com.torii.model.SearchPrecision;
import com.torii.model.SearchRequest;
import com.torii.user.PlanQuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Search orchestration: runs the algorithm and takes care of the side effects of each
 * search, i.e. the route's price history and the search history (anonymous or for
 * the logged in user). The REST controller never talks to the algorithm directly,
 * only to this service.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final SlidingWindowEngine engine;
    private final PriceHistoryService priceHistory;
    private final SearchHistoryService searchHistory;
    private final PlanQuotaService quota;
    private final PriceVerdictService verdicts;
    private final BestDatesRefiner refiner;

    public SearchService(SlidingWindowEngine engine, PriceHistoryService priceHistory,
                         SearchHistoryService searchHistory, PlanQuotaService quota,
                         PriceVerdictService verdicts, BestDatesRefiner refiner) {
        this.engine = engine;
        this.verdicts = verdicts;
        this.refiner = refiner;
        this.priceHistory = priceHistory;
        this.searchHistory = searchHistory;
        this.quota = quota;
    }

    /**
     * @param userId authenticated user id, or {@code null} for anonymous searches
     */
    public List<FlightOffer> search(SearchRequest request, @Nullable Long userId) {
        // The plan decides the precision: an unrequested one becomes the plan's best,
        // one the plan doesn't include is refused with a 403. Getaways ignore precision
        // (the step is always a week), so they just take the plan's best.
        if (userId != null) {
            request = request.withPrecision(request.hasWeekPattern()
                    ? quota.resolvePrecision(userId, null)
                    : quota.resolvePrecision(userId, request.precision()));
        } else if (request.precision() == null) {
            request = request.withPrecision(SearchPrecision.EXHAUSTIVE);
        }

        // Quota is checked and consumed BEFORE doing any work: if there's none left we
        // reject with a 429 without spending a single external call. This does NOT
        // go in the try/catch below, quota is a business rule, not a nice-to-have.
        if (userId != null) {
            quota.consume(userId, engine.countQueries(request));
        }

        SlidingWindowEngine.SearchResult result = engine.run(request);
        // Second look at the cheapest dates with a source that sees more fares (see
        // BestDatesRefiner). It's our cost, not the user's: it doesn't touch the quota.
        try {
            result = refiner.refine(request, result);
        } catch (RuntimeException e) {
            log.warn("No se pudo refinar la búsqueda: {}", e.getMessage());
        }
        List<FlightOffer> offers = result.offers();

        // Torii's own verdict for offers the source didn't judge. Before recording
        // today's observation, so a search is never compared with itself.
        try {
            offers = verdicts.addVerdicts(request.origin(), request.destination(), offers, result.cheapestPerDate());
        } catch (RuntimeException e) {
            log.warn("No se pudo calcular el veredicto de precio: {}", e.getMessage());
        }

        // Recording is best effort: if the DB is down the search should still answer.
        try {
            priceHistory.recordObservation(request.origin(), request.destination(), offers);
            searchHistory.record(request, userId, engine.countQueries(request));
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar la búsqueda en BD: {}", e.getMessage());
        }

        return offers;
    }
}
