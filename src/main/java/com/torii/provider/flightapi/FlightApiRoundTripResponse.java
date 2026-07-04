package com.torii.provider.flightapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Subconjunto de la respuesta del endpoint {@code /roundtrip} de FlightAPI.io.
 *
 * <p>A diferencia de SerpApi (que devuelve cada resultado "autocontenido"), FlightAPI
 * usa el modelo <b>normalizado</b> de Skyscanner: las listas se referencian entre sí
 * por IDs para no repetir datos. Un {@code itinerary} apunta a sus {@code legs} (ida y
 * vuelta) por {@code leg_ids}; cada leg apunta a aerolíneas ({@code carriers}) y
 * aeropuertos ({@code places}) por IDs numéricos. Para mapearlo a nuestro
 * {@link com.torii.model.FlightOffer} hay que "resolver" esas referencias con mapas
 * id → objeto, como una mini base de datos en memoria.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FlightApiRoundTripResponse(
        List<Itinerary> itineraries,
        List<Leg> legs,
        List<Carrier> carriers,
        List<Place> places
) {

    /** Una combinación concreta de ida + vuelta, con su precio más barato. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Itinerary(
            String id,
            @JsonProperty("leg_ids") List<String> legIds,
            @JsonProperty("pricing_options") List<PricingOption> pricingOptions,
            @JsonProperty("cheapest_price") Price cheapestPrice
    ) {}

    /** Una oferta de una agencia (OTA) para el itinerario, con su enlace de reserva. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PricingOption(Price price, List<Item> items) {}

    /** El {@code url} es un deep-link RELATIVO de Skyscanner (empieza por "/"). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String url, Price price) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Price(Double amount) {}

    /**
     * Un trayecto completo (la ida o la vuelta). {@code stop_ids} es una lista de
     * listas: los IDs de los aeropuertos de escala, agrupados por parada.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Leg(
            String id,
            String departure,
            @JsonProperty("stop_count") Integer stopCount,
            @JsonProperty("marketing_carrier_ids") List<Long> marketingCarrierIds,
            @JsonProperty("stop_ids") List<List<Long>> stopIds
    ) {}

    /** Aerolínea. Los IDs de carrier de Skyscanner suelen ser números negativos. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Carrier(
            Long id,
            String name,
            @JsonProperty("display_code") String displayCode
    ) {}

    /**
     * Aeropuerto/ciudad. El código IATA puede venir en {@code display_code} o en
     * {@code alt_id} según la versión de la respuesta, por eso guardamos ambos.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Place(
            Long id,
            @JsonProperty("display_code") String displayCode,
            @JsonProperty("alt_id") String altId,
            String name
    ) {

        /** Mejor código disponible para mostrar como escala. */
        public String bestCode() {
            if (displayCode != null && !displayCode.isBlank()) return displayCode;
            if (altId != null && !altId.isBlank()) return altId;
            return name;
        }
    }
}
