package com.torii.search;

import com.torii.algorithm.SlidingWindowEngine;
import com.torii.history.PriceHistoryService;
import com.torii.model.FlightOffer;
import com.torii.model.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquestador de búsquedas: ejecuta el algoritmo y registra los "efectos
 * secundarios" de cada búsqueda (histórico de precios; próximamente, historial de
 * búsquedas del usuario y consumo de cuota del plan). El controlador REST nunca
 * habla con el algoritmo directamente, solo con este servicio.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final SlidingWindowEngine engine;
    private final PriceHistoryService priceHistory;

    public SearchService(SlidingWindowEngine engine, PriceHistoryService priceHistory) {
        this.engine = engine;
        this.priceHistory = priceHistory;
    }

    public List<FlightOffer> search(SearchRequest request) {
        List<FlightOffer> offers = engine.findBestOffers(request);

        // El histórico es un extra: si la BD fallara, la búsqueda debe responder igual.
        try {
            priceHistory.recordObservation(request.origin(), request.destination(), offers);
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar el histórico de precios: {}", e.getMessage());
        }

        return offers;
    }
}
