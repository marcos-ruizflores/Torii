package com.torii.api;

import com.torii.model.FlightOffer;
import com.torii.search.SearchService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HTTP entry point for searches.
 *
 * <p>Takes the request as a {@link SearchRequestDto} (checked by {@code @Valid}),
 * maps it to the domain and hands it to {@link SearchService}. No business logic
 * here, it's just the boundary between HTTP and the domain.
 *
 * <p>Searching doesn't need an account, but if the request carries a valid JWT,
 * Spring injects it as the principal and the search gets linked to the user (for
 * "recent searches"). Without a token {@code jwt} is null and it's an anonymous search.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public List<FlightOffer> search(@Valid @RequestBody SearchRequestDto dto,
                                    @AuthenticationPrincipal Jwt jwt) {
        Long userId = (jwt != null) ? Long.valueOf(jwt.getSubject()) : null;
        return searchService.search(dto.toDomain(), userId);
    }
}
