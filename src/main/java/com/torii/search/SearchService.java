package com.torii.search;

import com.torii.algorithm.SlidingWindowEngine;
import com.torii.history.PriceHistoryService;
import com.torii.history.SearchHistoryService;
import com.torii.model.FlightOffer;
import com.torii.model.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquestador de búsquedas: ejecuta el algoritmo y registra los "efectos
 * secundarios" de cada búsqueda — el histórico de precios de la ruta y el
 * historial de búsquedas (anónimo o del usuario autenticado). El controlador REST
 * nunca habla con el algoritmo directamente, solo con este servicio.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final SlidingWindowEngine engine;
    private final PriceHistoryService priceHistory;
    private final SearchHistoryService searchHistory;

    public SearchService(SlidingWindowEngine engine, PriceHistoryService priceHistory,
                         SearchHistoryService searchHistory) {
        this.engine = engine;
        this.priceHistory = priceHistory;
        this.searchHistory = searchHistory;
    }

    /**
     * @param userId id del usuario autenticado, o {@code null} si la búsqueda es anónima
     */
    public List<FlightOffer> search(SearchRequest request, @Nullable Long userId) {
        List<FlightOffer> offers = engine.findBestOffers(request);

        // Los registros son extras: si la BD fallara, la búsqueda debe responder igual.
        try {
            priceHistory.recordObservation(request.origin(), request.destination(), offers);
            searchHistory.record(request, userId, engine.countQueries(request));
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar la búsqueda en BD: {}", e.getMessage());
        }

        return offers;
    }
}
