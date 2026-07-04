package com.torii.search;

import com.torii.algorithm.SlidingWindowEngine;
import com.torii.model.FlightOffer;
import com.torii.model.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquestador de búsquedas.
 *
 * <p>De momento es una fina capa sobre el {@link SlidingWindowEngine}, pero existe a
 * propósito: es el sitio natural donde, en fases siguientes, vivirá la lógica de
 * caché (mirar Redis antes de lanzar el algoritmo), el cálculo del TTL variable
 * según la cercanía del viaje, el registro del histórico en base de datos, etc. El
 * controlador REST nunca hablará con el algoritmo directamente, solo con este
 * servicio.
 */
@Service
public class SearchService {

    private final SlidingWindowEngine engine;

    public SearchService(SlidingWindowEngine engine) {
        this.engine = engine;
    }

    public List<FlightOffer> search(SearchRequest request) {
        // Futuro: 1) consultar caché → 2) si falla, ejecutar engine → 3) guardar en caché con TTL.
        return engine.findBestOffers(request);
    }
}
