package com.torii.provider.flightapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The part of the FlightAPI.io {@code /roundtrip} response we use.
 *
 * <p>Unlike SerpApi, where each result is self-contained, FlightAPI uses Skyscanner's
 * <b>normalized</b> model: lists reference each other by ID to avoid repeating data.
 * An {@code itinerary} points to its {@code legs} (outbound and return) through
 * {@code leg_ids}, and each leg points to {@code carriers} and {@code places} by
 * numeric ID. To map it to our {@link com.torii.model.FlightOffer} those references
 * get resolved with id -> object maps, kind of like a tiny in-memory database.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FlightApiRoundTripResponse(
        List<Itinerary> itineraries,
        List<Leg> legs,
        List<Carrier> carriers,
        List<Place> places
) {

    /** A specific outbound + return combination with its cheapest price. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Itinerary(
            String id,
            @JsonProperty("leg_ids") List<String> legIds,
            @JsonProperty("pricing_options") List<PricingOption> pricingOptions,
            @JsonProperty("cheapest_price") Price cheapestPrice
    ) {}

    /** One agency (OTA) offer for the itinerary, with its booking link. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PricingOption(Price price, List<Item> items) {}

    /** {@code url} is a RELATIVE Skyscanner deep link (starts with "/"). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String url, Price price) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Price(Double amount) {}

    /**
     * A full leg (outbound or return). {@code stop_ids} is a list of lists: stopover
     * airport IDs grouped by stop.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Leg(
            String id,
            String departure,
            @JsonProperty("stop_count") Integer stopCount,
            @JsonProperty("marketing_carrier_ids") List<Long> marketingCarrierIds,
            @JsonProperty("stop_ids") List<List<Long>> stopIds
    ) {}

    /** Airline. Skyscanner carrier IDs are usually negative numbers. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Carrier(
            Long id,
            String name,
            @JsonProperty("display_code") String displayCode
    ) {}

    /**
     * Airport or city. The IATA code can show up in {@code display_code} or in
     * {@code alt_id} depending on the response version, so we keep both.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Place(
            Long id,
            @JsonProperty("display_code") String displayCode,
            @JsonProperty("alt_id") String altId,
            String name
    ) {

        /** Best available code to display as a stopover. */
        public String bestCode() {
            if (displayCode != null && !displayCode.isBlank()) return displayCode;
            if (altId != null && !altId.isBlank()) return altId;
            return name;
        }
    }
}
