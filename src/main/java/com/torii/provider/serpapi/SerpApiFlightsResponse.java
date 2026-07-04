package com.torii.provider.serpapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Subconjunto de la respuesta de SerpApi (engine {@code google_flights}).
 *
 * <p>SerpApi agrupa los resultados en {@code best_flights} (los que destaca Google) y
 * {@code other_flights} (el resto). Cada grupo tiene un precio, una lista de tramos
 * ({@code flights}) y una lista de escalas ({@code layovers}). Si algo va mal, SerpApi
 * suele incluir un campo {@code error} en el JSON.
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

    /** Aeropuerto con su código y la hora ("AAAA-MM-DD HH:MM"). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Airport(String id, String time) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Layover(String id, String name) {}
}
