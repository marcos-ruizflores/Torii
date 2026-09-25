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
 * FAKE {@link FlightProvider} for developing and testing the algorithm without
 * depending on any external API.
 *
 * <p>Prices are <b>deterministic</b>: the same date pair always returns the same
 * result. Two big wins from that:
 * <ul>
 *   <li>Algorithm tests are reproducible.</li>
 *   <li>We can actually check that a cache hit returns the same thing as the
 *       source.</li>
 * </ul>
 *
 * <p>It also fakes a bit of realism: August is more expensive, weekend departures
 * cost more and extra stops make it cheaper. None of it is real, but it makes the
 * top 5 look believable during development.
 */
@Component
public class MockFlightProvider implements FlightProvider {

    private static final String[] AIRLINES = {
            "Iberia", "Vueling", "Ryanair", "Lufthansa", "Air France", "KLM"
    };

    /** Common hub airports used to fake stopovers. */
    private static final String[] HUBS = {"CDG", "FRA", "AMS", "IST", "DXB", "DOH"};

    @Override
    public List<FlightOffer> searchOffers(
            String origin,
            String destination,
            LocalDate departDate,
            LocalDate returnDate,
            int maxStops
    ) {
        // Seed derived from the query itself: same dates, same result every time.
        long seed = (origin + destination + departDate + returnDate).hashCode();
        Random rng = new Random(seed);

        List<FlightOffer> offers = new ArrayList<>();
        int howMany = 2 + rng.nextInt(3); // 2 to 4 offers per date pair

        for (int i = 0; i < howMany; i++) {
            int stops = rng.nextInt(maxStops + 1); // 0 to maxStops stops
            String airline = AIRLINES[rng.nextInt(AIRLINES.length)];
            BigDecimal price = fakePrice(departDate, stops, rng);

            // Fake departure times for both legs, between 6:00 and 22:00.
            LocalTime departureTime = LocalTime.of(6 + rng.nextInt(16), rng.nextBoolean() ? 0 : 30);
            LocalTime returnDepartureTime = LocalTime.of(6 + rng.nextInt(16), rng.nextBoolean() ? 0 : 30);

            // Fake stopover airports, one per stop.
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
     * Fake price with some rough market logic:
     * base + seasonality (August is pricey) + weekend surcharge - discount per stop.
     */
    private BigDecimal fakePrice(LocalDate departDate, int stops, Random rng) {
        double base = 180 + rng.nextInt(120); // 180-299 EUR

        // Seasonality: August (month 8) is the most expensive, the further away the cheaper.
        int monthDistanceToAugust = Math.abs(departDate.getMonthValue() - 8);
        double seasonal = (4 - Math.min(4, monthDistanceToAugust)) * 60; // up to +240 EUR in August

        // Leaving on Friday or Saturday costs more.
        DayOfWeek dow = departDate.getDayOfWeek();
        double weekend = (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY) ? 45 : 0;

        // Each stop makes it cheaper (direct flights cost more).
        double stopsDiscount = stops * 35;

        double total = Math.max(39, base + seasonal + weekend - stopsDiscount);
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
    }
}
