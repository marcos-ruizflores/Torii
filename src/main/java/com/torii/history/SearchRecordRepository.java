package com.torii.history;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Access to the {@code searches} table. */
public interface SearchRecordRepository extends JpaRepository<SearchRecord, Long> {

    /** A user's most recent searches (the Pageable sets the limit). */
    List<SearchRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
