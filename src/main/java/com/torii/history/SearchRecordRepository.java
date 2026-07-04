package com.torii.history;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a la tabla {@code searches}. */
public interface SearchRecordRepository extends JpaRepository<SearchRecord, Long> {

    /** Las búsquedas más recientes de un usuario (el Pageable pone el tope). */
    List<SearchRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
