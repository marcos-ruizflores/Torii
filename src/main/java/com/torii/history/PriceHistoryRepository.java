package com.torii.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Access to {@code price_history}. Spring Data derives the queries from the method
 * NAMES, so there's no SQL to write.
 */
public interface PriceHistoryRepository extends JpaRepository<PriceHistoryEntry, Long> {

    Optional<PriceHistoryEntry> findByOriginAndDestinationAndDay(
            String origin, String destination, LocalDate day);

    List<PriceHistoryEntry> findByOriginAndDestinationAndDayGreaterThanEqualOrderByDayAsc(
            String origin, String destination, LocalDate from);
}
