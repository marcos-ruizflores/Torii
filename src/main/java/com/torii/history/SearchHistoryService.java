package com.torii.history;

import com.torii.model.SearchRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Search history: stores every search (anonymous or from a user) and serves each
 * account's latest ones for "recent searches".
 */
@Service
public class SearchHistoryService {

    private final SearchRecordRepository repository;

    public SearchHistoryService(SearchRecordRepository repository) {
        this.repository = repository;
    }

    /** Stores the search. A null {@code userId} means anonymous. */
    @Transactional
    public void record(SearchRequest request, Long userId, int queriesUsed) {
        repository.save(new SearchRecord(userId, request, queriesUsed));
    }

    /** User's last {@code limit} searches, newest first. */
    @Transactional(readOnly = true)
    public List<SavedSearchDto> recentSearches(Long userId, int limit) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit)).stream()
                .map(SavedSearchDto::from)
                .toList();
    }
}
