package com.torii.history;

import com.torii.model.SearchRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Historial de búsquedas: guarda cada búsqueda (anónima o de un usuario) y sirve
 * las últimas de cada cuenta para "Mis últimas búsquedas".
 */
@Service
public class SearchHistoryService {

    private final SearchRecordRepository repository;

    public SearchHistoryService(SearchRecordRepository repository) {
        this.repository = repository;
    }

    /** Guarda la búsqueda. {@code userId} null = búsqueda anónima. */
    @Transactional
    public void record(SearchRequest request, Long userId, int queriesUsed) {
        repository.save(new SearchRecord(userId, request, queriesUsed));
    }

    /** Las últimas {@code limit} búsquedas del usuario, la más reciente primero. */
    @Transactional(readOnly = true)
    public List<SavedSearchDto> recentSearches(Long userId, int limit) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit)).stream()
                .map(SavedSearchDto::from)
                .toList();
    }
}
