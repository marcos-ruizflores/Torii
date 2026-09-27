package com.torii.provider.flightpowers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One item of the FlightPowers round-trip response
 * ({@code POST /api/google_flights/roundtrip/v1}), which is a plain JSON array.
 *
 * <p>Unlike FlightAPI, every result is self-contained: price, both legs and Google's
 * price verdict all come flattened in the same object. Times come as text in
 * English ("5:05 PM on Tue, Oct 6"). The shape of {@code *_stops_info} items isn't
 * documented, so they're read loosely and the provider pulls the IATA code out of
 * whatever it finds.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FlightPowersRoundTripOffer(
        @JsonProperty("total_price_as_number") Double totalPrice,
        @JsonProperty("total_stops") Integer totalStops,
        @JsonProperty("buy_link") String buyLink,

        @JsonProperty("departure_flight_airline") String departureAirline,
        @JsonProperty("departure_flight_stops") Integer departureStops,
        @JsonProperty("departure_flight_departure_description") String departureDescription,
        @JsonProperty("departure_stops_info") List<Object> departureStopsInfo,

        @JsonProperty("return_flight_departure_description") String returnDepartureDescription,

        @JsonProperty("price_range_in_relation_to_other_periods") String priceLevel,
        @JsonProperty("price_insights_low") Double priceInsightsLow,
        @JsonProperty("price_insights_high") Double priceInsightsHigh
) {}
