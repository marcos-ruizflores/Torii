package com.torii.search;

import com.torii.algorithm.SlidingWindowEngine.SearchResult;
import com.torii.model.FlightOffer;
import com.torii.model.SearchRequest;
import com.torii.provider.FlightProvider;
import com.torii.provider.ProviderQuotaExceededException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Second look at the cheapest dates with another source, merged into the results. */
class BestDatesRefinerTest {

    private static final LocalDate D1 = LocalDate.of(2026, 12, 1);

    private static FlightOffer offer(String airline, int price, int departDay) {
        LocalDate depart = D1.plusDays(departDay);
        return new FlightOffer(airline, BigDecimal.valueOf(price), "EUR", 1, depart, depart.plusDays(13),
                "https://www.google.com/travel/flights");
    }

    private static SearchRequest request(int topN) {
        return new SearchRequest("BCN", "NRT", D1, D1.plusDays(20), 13, 0, 1, topN);
    }

    /** Fake second source: always the same answer, counting calls. */
    private static FlightProvider source(List<FlightOffer> answer, AtomicInteger calls, boolean outOfQuota) {
        return new FlightProvider() {
            @Override
            public List<FlightOffer> searchOffers(String o, String d, LocalDate dep, LocalDate ret, int stops) {
                calls.incrementAndGet();
                if (outOfQuota) throw new ProviderQuotaExceededException("sin cuota");
                return answer.stream().filter(a -> a.departDate().equals(dep)).toList();
            }

            @Override
            public String name() {
                return "SerpApi";
            }
        };
    }

    @Test
    void laTarifaMasBarataDeLaSegundaFuenteEntraEnLosResultados() {
        // FlightPowers only saw the highlighted fares; SerpApi also has Air China at 575.
        List<FlightOffer> scanned = List.of(offer("Air France", 729, 0), offer("KLM", 752, 1), offer("Etihad", 755, 2),
                offer("Qatar", 900, 3));
        SearchResult result = new SearchResult(List.of(offer("Air France", 729, 0), offer("KLM", 752, 1)), scanned);
        AtomicInteger calls = new AtomicInteger();
        BestDatesRefiner refiner = new BestDatesRefiner(
                source(List.of(offer("Air China", 575, 0), offer("Air France", 729, 0)), calls, false), 3, Clock.systemUTC());

        SearchResult refined = refiner.refine(request(2), result);

        assertThat(calls.get()).isEqualTo(3); // only the 3 cheapest dates, not all 4
        assertThat(refined.offers()).extracting(FlightOffer::airline).containsExactly("Air China", "Air France");
        assertThat(refined.cheapestPerDate()).extracting(o -> o.price().intValue()).contains(575).doesNotContain(729);
    }

    @Test
    void sinCuotaDevuelveLoQueHabiaYDejaDeIntentarlo() {
        SearchResult result = new SearchResult(List.of(offer("KLM", 752, 1)), List.of(offer("KLM", 752, 1)));
        AtomicInteger calls = new AtomicInteger();
        BestDatesRefiner refiner = new BestDatesRefiner(source(List.of(), calls, true), 3, Clock.systemUTC());

        assertThat(refiner.refine(request(5), result)).isEqualTo(result);
        assertThat(refiner.refine(request(5), result)).isEqualTo(result);
        assertThat(calls.get()).isEqualTo(1); // paused after the quota error
    }

    @Test
    void desactivadoNoHaceNada() {
        SearchResult result = new SearchResult(new ArrayList<>(), List.of(offer("KLM", 752, 1)));
        assertThat(BestDatesRefiner.disabled().refine(request(5), result)).isSameAs(result);
    }
}
