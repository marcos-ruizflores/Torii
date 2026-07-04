package com.torii.api;

import com.torii.model.FlightOffer;
import com.torii.search.SearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Punto de entrada HTTP de Torii.
 *
 * <p>Recibe la petición como {@link SearchRequestDto} (validada por {@code @Valid}),
 * la traduce al dominio y delega en {@link SearchService}. No contiene lógica de
 * negocio: solo es la frontera entre el mundo HTTP y el dominio.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public List<FlightOffer> search(@Valid @RequestBody SearchRequestDto dto) {
        return searchService.search(dto.toDomain());
    }
}
