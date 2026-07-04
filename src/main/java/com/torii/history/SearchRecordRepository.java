package com.torii.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a la tabla {@code searches}. */
public interface SearchRecordRepository extends JpaRepository<SearchRecord, Long> {

    /** Las 10 búsquedas más recientes de un usuario, para "Mis últimas búsquedas". */
    List<SearchRecord> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
