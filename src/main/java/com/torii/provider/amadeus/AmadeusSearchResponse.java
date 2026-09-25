package com.torii.provider.amadeus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The part of the Amadeus search response ({@code GET /v2/shopping/flight-offers})
 * we actually use.
 *
 * <p>The real response is huge. This only models what's needed to build a
 * {@link com.torii.model.FlightOffer}: price, itineraries with their segments and
 * the validating airline. Everything else is ignored.
 *
 * <p>Shape: {@code data} is the list of offers. Each offer has a {@code price} and a
 * list of {@code itineraries} (outbound and return), and each itinerary has
 * {@code segments} (one segment per flight, several segments means stops).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AmadeusSearchResponse(List<Offer> data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Offer(
            Price price,
            List<Itinerary> itineraries,
            List<String> validatingAirlineCodes
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Price(String grandTotal, String currency) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Itinerary(List<Segment> segments) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Segment(Endpoint departure, Endpoint arrival) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Endpoint(String iataCode, String at) {}
}
