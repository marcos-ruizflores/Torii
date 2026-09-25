package com.torii.provider.serpapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The part of the SerpApi response ({@code google_flights} engine) we use.
 *
 * <p>Results come split into {@code best_flights} (the ones Google highlights) and
 * {@code other_flights} (everything else). Each group has a price, a list of legs
 * ({@code flights}) and a list of {@code layovers}. When something goes wrong SerpApi
 * usually adds an {@code error} field to the JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SerpApiFlightsResponse(
        @JsonProperty("best_flights") List<FlightGroup> bestFlights,
        @JsonProperty("other_flights") List<FlightGroup> otherFlights,
        String error
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FlightGroup(
            List<Leg> flights,
            List<Layover> layovers,
            Integer price
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Leg(
            String airline,
            @JsonProperty("departure_airport") Airport departureAirport
    ) {}

    /** Airport code and time ("YYYY-MM-DD HH:MM"). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Airport(String id, String time) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Layover(String id, String name) {}
}
