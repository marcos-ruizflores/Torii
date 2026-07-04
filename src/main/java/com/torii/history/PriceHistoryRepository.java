package com.torii.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code price_history}. Spring Data genera la implementación a
 * partir del NOMBRE de cada método (query derivation): no escribimos SQL.
 */
public interface PriceHistoryRepository extends JpaRepository<PriceHistoryEntry, Long> {

    Optional<PriceHistoryEntry> findByOriginAndDestinationAndDay(
            String origin, String destination, LocalDate day);

    List<PriceHistoryEntry> findByOriginAndDestinationAndDayGreaterThanEqualOrderByDayAsc(
            String origin, String destination, LocalDate from);
}
