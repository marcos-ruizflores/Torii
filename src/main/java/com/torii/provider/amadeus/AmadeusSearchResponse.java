package com.torii.provider.amadeus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Subconjunto de la respuesta de búsqueda de Amadeus
 * ({@code GET /v2/shopping/flight-offers}) que nos interesa.
 *
 * <p>La respuesta real es enorme; aquí modelamos solo lo necesario para construir un
 * {@link com.torii.model.FlightOffer}: precio, trayectos (itinerarios con sus
 * segmentos) y la aerolínea validante. Todo lo demás se ignora.
 *
 * <p>Estructura: {@code data} es la lista de ofertas. Cada oferta tiene un
 * {@code price} y una lista de {@code itineraries} (ida y vuelta); cada itinerario
 * tiene {@code segments} (cada segmento es un vuelo; varios segmentos = escalas).
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
