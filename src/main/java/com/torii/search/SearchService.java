package com.torii.search;

import com.torii.algorithm.SlidingWindowEngine;
import com.torii.history.PriceHistoryService;
import com.torii.history.SearchHistoryService;
import com.torii.model.FlightOffer;
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

    public SearchService(SlidingWindowEngine engine, PriceHistoryService priceHistory,
                         SearchHistoryService searchHistory, PlanQuotaService quota) {
        this.engine = engine;
        this.priceHistory = priceHistory;
        this.searchHistory = searchHistory;
        this.quota = quota;
    }

    /**
     * @param userId authenticated user id, or {@code null} for anonymous searches
     */
    public List<FlightOffer> search(SearchRequest request, @Nullable Long userId) {
        // Quota is checked and consumed BEFORE doing any work: if there's none left we
        // reject with a 429 without spending a single external call. This does NOT
        // go in the try/catch below, quota is a business rule, not a nice-to-have.
        if (userId != null) {
            quota.consume(userId, engine.countQueries(request));
        }

        List<FlightOffer> offers = engine.findBestOffers(request);

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
