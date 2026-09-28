package com.torii.history;

import com.torii.model.FlightOffer;
import com.torii.model.PriceInsight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Torii's own price verdict, against stored history or the dates scanned. */
class PriceVerdictServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

    private PriceHistoryRepository repository;
    private PriceVerdictService service;

    @BeforeEach
    void setUp() {
        repository = mock(PriceHistoryRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC);
        service = new PriceVerdictService(repository, clock);
    }

    private static FlightOffer offer(int price) {
        return new FlightOffer("Etihad", BigDecimal.valueOf(price), "EUR", 1,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 13), "https://www.google.com/travel/flights");
    }

    private void history(int... prices) {
        List<PriceHistoryEntry> rows = IntStream.range(0, prices.length)
                .mapToObj(i -> new PriceHistoryEntry("BCN", "NRT", TODAY.minusDays(prices.length - i),
                        BigDecimal.valueOf(prices[i]), "EUR", "GoogleFlights"))
                .toList();
        when(repository.findByOriginAndDestinationAndDayGreaterThanEqualOrderByDayAsc(eq("BCN"), eq("NRT"), any()))
                .thenReturn(rows);
    }

    @Test
    void conHistoricoSuficienteJuzgaContraElHistorico() {
        history(700, 720, 750, 780, 800, 820, 850, 900);

        List<FlightOffer> judged = service.addVerdicts("BCN", "NRT",
                List.of(offer(690), offer(790), offer(950)), List.of());

        assertThat(judged).extracting(o -> o.priceInsight().level()).containsExactly("low", "typical", "high");
        PriceInsight insight = judged.get(0).priceInsight();
        assertThat(insight.source()).isEqualTo(PriceInsight.HISTORY);
        assertThat(insight.samples()).isEqualTo(8);
        assertThat(insight.typicalLow()).isEqualByComparingTo("720");
        assertThat(insight.typicalHigh()).isEqualByComparingTo("820");
    }

    @Test
    void elDiaDeHoyNoCuentaComoHistorico() {
        List<PriceHistoryEntry> rows = List.of(
                new PriceHistoryEntry("BCN", "NRT", TODAY.minusDays(2), BigDecimal.valueOf(700), "EUR", null),
                new PriceHistoryEntry("BCN", "NRT", TODAY.minusDays(1), BigDecimal.valueOf(720), "EUR", null),
                new PriceHistoryEntry("BCN", "NRT", TODAY.minusDays(3), BigDecimal.valueOf(740), "EUR", null),
                new PriceHistoryEntry("BCN", "NRT", TODAY.minusDays(4), BigDecimal.valueOf(760), "EUR", null),
                new PriceHistoryEntry("BCN", "NRT", TODAY, BigDecimal.valueOf(500), "EUR", null));
        when(repository.findByOriginAndDestinationAndDayGreaterThanEqualOrderByDayAsc(any(), any(), any()))
                .thenReturn(rows);

        // Only 4 past days: not enough history, and too few scanned dates either.
        List<FlightOffer> judged = service.addVerdicts("BCN", "NRT", List.of(offer(700)), List.of(offer(700)));

        assertThat(judged.get(0).priceInsight()).isNull();
    }

    @Test
    void sinHistoricoUsaLasFechasEscaneadas() {
        history();
        List<FlightOffer> scanned = List.of(offer(762), offer(780), offer(800), offer(850), offer(888), offer(900));

        List<FlightOffer> judged = service.addVerdicts("BCN", "NRT", List.of(offer(762)), scanned);

        PriceInsight insight = judged.get(0).priceInsight();
        assertThat(insight.level()).isEqualTo("low");
        assertThat(insight.source()).isEqualTo(PriceInsight.SCAN);
        assertThat(insight.samples()).isEqualTo(6);
    }

    @Test
    void conPreciosIgualesEsHabitualNoBajo() {
        history(750, 750, 750, 750, 750);

        List<FlightOffer> judged = service.addVerdicts("BCN", "NRT", List.of(offer(750)), List.of());

        assertThat(judged.get(0).priceInsight().level()).isEqualTo("typical");
    }

    @Test
    void respetaElVeredictoDeLaFuenteYNoJuzgaOfertasMock() {
        history(700, 720, 750, 780, 800);
        FlightOffer fromGoogle = offer(800).withPriceInsight(
                new PriceInsight("low", BigDecimal.valueOf(610), BigDecimal.valueOf(1050)));
        FlightOffer mock = new FlightOffer("Mock", BigDecimal.valueOf(100), "EUR", 0,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 13), "https://example.com/book");

        List<FlightOffer> judged = service.addVerdicts("BCN", "NRT", List.of(fromGoogle, mock, offer(900)), List.of());

        assertThat(judged.get(0).priceInsight().source()).isEqualTo(PriceInsight.GOOGLE);
        assertThat(judged.get(1).priceInsight()).isNull();
        assertThat(judged.get(2).priceInsight().level()).isEqualTo("high");
    }
}
