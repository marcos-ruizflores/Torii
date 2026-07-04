package com.torii.history;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Histórico de precios de una ruta, para el gráfico del frontend:
 * {@code GET /api/price-history?origin=BCN&destination=NRT&days=30}.
 */
@RestController
@RequestMapping("/api/price-history")
public class PriceHistoryController {

    private final PriceHistoryService service;

    public PriceHistoryController(PriceHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<PricePointDto> history(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(defaultValue = "30") int days) {

        // Normalización defensiva: mismos límites que tiene el gráfico (7-365 días).
        int clampedDays = Math.max(1, Math.min(days, 365));
        return service.history(origin.toUpperCase(), destination.toUpperCase(), clampedDays);
    }
}
