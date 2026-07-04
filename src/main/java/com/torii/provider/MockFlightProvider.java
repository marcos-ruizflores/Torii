package com.torii.provider;

import com.torii.model.FlightOffer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implementación FALSA de {@link FlightProvider} para desarrollar y probar el
 * algoritmo sin depender de ninguna API externa.
 *
 * <p>Genera precios <b>deterministas</b>: para un mismo par de fechas siempre
 * devuelve el mismo resultado. Esto tiene dos ventajas grandes:
 * <ul>
 *   <li>Los tests del algoritmo son reproducibles.</li>
 *   <li>Cuando metamos caché (Caffeine/Redis) más adelante, podremos comprobar de
 *       verdad que un acierto de caché devuelve lo mismo que la fuente.</li>
 * </ul>
 *
 * <p>Además simula un poco de realismo: la estacionalidad encarece agosto, salir en
 * fin de semana sube el precio, y más escalas abaratan. Nada de esto es real, pero
 * hace que el "top 5" del algoritmo se vea creíble durante el desarrollo.
 */
@Component
public class MockFlightProvider implements FlightProvider {

    private static final String[] AIRLINES = {
            "Iberia", "Vueling", "Ryanair", "Lufthansa", "Air France", "KLM"
    };

    /** Aeropuertos "hub" típicos para simular escalas. */
    private static final String[] HUBS = {"CDG", "FRA", "AMS", "IST", "DXB", "DOH"};

    @Override
    public List<FlightOffer> searchOffers(
            String origin,
            String destination,
            LocalDate departDate,
            LocalDate returnDate,
            int maxStops
    ) {
        // Semilla determinista a partir de los datos de la consulta: mismas fechas,
        // mismo resultado siempre.
        long seed = (origin + destination + departDate + returnDate).hashCode();
        Random rng = new Random(seed);

        List<FlightOffer> offers = new ArrayList<>();
        int howMany = 2 + rng.nextInt(3); // entre 2 y 4 ofertas por par de fechas

        for (int i = 0; i < howMany; i++) {
            int stops = rng.nextInt(maxStops + 1); // de 0 a maxStops escalas
            String airline = AIRLINES[rng.nextInt(AIRLINES.length)];
            BigDecimal price = fakePrice(departDate, stops, rng);

            // Horas de salida simuladas, ida y vuelta (entre las 6:00 y las 22:00).
            LocalTime departureTime = LocalTime.of(6 + rng.nextInt(16), rng.nextBoolean() ? 0 : 30);
            LocalTime returnDepartureTime = LocalTime.of(6 + rng.nextInt(16), rng.nextBoolean() ? 0 : 30);

            // Aeropuertos de escala simulados (tantos como "stops").
            List<String> stopovers = new ArrayList<>();
            for (int s = 0; s < stops; s++) {
                stopovers.add(HUBS[rng.nextInt(HUBS.length)]);
            }

            String url = "https://example.com/booking?from=%s&to=%s&out=%s&in=%s&airline=%s"
                    .formatted(origin, destination, departDate, returnDate,
                            airline.replace(" ", "%20"));

            offers.add(new FlightOffer(
                    airline, price, "EUR", stops, departDate, returnDate,
                    departureTime, returnDepartureTime, stopovers, url));
        }
        return offers;
    }

    /**
     * Precio simulado con un poco de "física" de mercado:
     * base + estacionalidad (agosto caro) + recargo de fin de semana − descuento por escalas.
     */
    private BigDecimal fakePrice(LocalDate departDate, int stops, Random rng) {
        double base = 180 + rng.nextInt(120); // 180–299 €

        // Estacionalidad: agosto (mes 8) es el más caro; cuanto más lejos de agosto, más barato.
        int monthDistanceToAugust = Math.abs(departDate.getMonthValue() - 8);
        double seasonal = (4 - Math.min(4, monthDistanceToAugust)) * 60; // hasta +240 € en agosto

        // Salir en viernes/sábado encarece.
        DayOfWeek dow = departDate.getDayOfWeek();
        double weekend = (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY) ? 45 : 0;

        // Cada escala abarata (vuelos directos son más caros).
        double stopsDiscount = stops * 35;

        double total = Math.max(39, base + seasonal + weekend - stopsDiscount);
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
    }
}
