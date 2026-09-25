package com.torii.provider;

import com.torii.model.FlightOffer;

import java.time.LocalDate;
import java.util.List;

/**
 * Abstraction over "where the flight data comes from".
 *
 * <p>This is the most important design decision in Torii: the rest of the system
 * (algorithm, service, API) depends on this contract and NOT on a specific source.
 * The mock, Amadeus, SerpApi and FlightAPI providers all implement it, and adding a
 * new one doesn't touch a single line of the algorithm.
 *
 * <p>Each call is a lookup for one pair of dates (outbound and return). The sliding
 * window algorithm calls this many times per search.
 */
public interface FlightProvider {

    /**
     * Looks up round-trip offers for a specific pair of dates.
     *
     * @param origin      origin IATA code (e.g. "BCN")
     * @param destination destination IATA code (e.g. "NRT")
     * @param departDate  outbound date
     * @param returnDate  return date
     * @param maxStops    max number of stops allowed
     * @return offers found (empty if there are no flights)
     */
    List<FlightOffer> searchOffers(
            String origin,
            String destination,
            LocalDate departDate,
            LocalDate returnDate,
            int maxStops
    );

    /**
     * Human readable provider name, used in logs and by the failover engine to track
     * which provider it's skipping or parking. Defaults to the class name.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
