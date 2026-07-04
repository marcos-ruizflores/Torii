package com.torii.provider;

import com.torii.model.FlightOffer;

import java.time.LocalDate;
import java.util.List;

/**
 * Abstracción de "de dónde salen los datos de vuelos".
 *
 * <p>Esta interfaz es la decisión de diseño más importante de Torii: todo el resto
 * del sistema (algoritmo, servicio, API) depende de este contrato y NO de una fuente
 * concreta. Hoy la implementa {@link MockFlightProvider} (datos falsos y
 * deterministas); mañana podremos añadir un {@code AmadeusFlightProvider} sin tocar
 * ni una línea del algoritmo.
 *
 * <p>Cada llamada representa una consulta de un único par de fechas (ida y vuelta).
 * El algoritmo de ventana deslizante invocará este método muchas veces.
 */
public interface FlightProvider {

    /**
     * Busca ofertas de ida y vuelta para un par de fechas concreto.
     *
     * @param origin      código IATA de origen (ej. "BCN")
     * @param destination código IATA de destino (ej. "NRT")
     * @param departDate  fecha de ida
     * @param returnDate  fecha de vuelta
     * @param maxStops    número máximo de escalas aceptadas
     * @return lista de ofertas encontradas (puede estar vacía si no hay vuelos)
     */
    List<FlightOffer> searchOffers(
            String origin,
            String destination,
            LocalDate departDate,
            LocalDate returnDate,
            int maxStops
    );

    /**
     * Nombre legible del proveedor, para logs y para que el motor de failover sepa
     * a quién está saltando o deshabilitando. Por defecto, el nombre de la clase.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
