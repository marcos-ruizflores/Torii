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
 * <p>Searching needs an account (the security chain rejects requests without a token
 * with a 401), so {@code jwt} is always set. Its {@code subject} is the user id, which
 * links the search to that user (quota, "recent searches").
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
        Long userId = Long.valueOf(jwt.getSubject());
        return searchService.search(dto.toDomain(), userId);
    }
}
