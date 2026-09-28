package com.torii.history;

import com.torii.model.FlightOffer;
import com.torii.model.PriceInsight;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Torii's own price verdict: is this offer low, typical or high?
 *
 * <p>Flight APIs mostly don't say (FlightPowers' round trips come with the verdict
 * fields empty), so Torii works it out from its own data:
 * <ol>
 *   <li><b>History</b>: the best price stored for the route on each of the last 90
 *       days, today excluded. Needs at least {@value #MIN_HISTORY_DAYS} days.</li>
 *   <li><b>Scan</b>, when there isn't that much history yet: the cheapest price of
 *       each date pair scanned in this same search. Needs at least
 *       {@value #MIN_SCANNED_DATES} dates. It says whether you're looking at the
 *       cheap dates of your window, not whether the route is cheap right now.</li>
 * </ol>
 * The usual range is the 25th to 75th percentile of the reference. At or under the
 * first it's low, at or over the second it's high, in between it's typical. With
 * less data than that there's no verdict: better nothing than a made-up one.
 *
 * <p>An offer that already carries a verdict from its source keeps it, and mock
 * offers (made-up prices) never get one.
 */
@Service
public class PriceVerdictService {

    static final int HISTORY_DAYS = 90;
    static final int MIN_HISTORY_DAYS = 5;
    static final int MIN_SCANNED_DATES = 6;

    private final PriceHistoryRepository history;
    private final Clock clock;

    public PriceVerdictService(PriceHistoryRepository history, Clock clock) {
        this.history = history;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<FlightOffer> addVerdicts(String origin, String destination,
                                         List<FlightOffer> offers, List<FlightOffer> cheapestPerDate) {
        if (offers.stream().allMatch(o -> o.priceInsight() != null || PriceHistoryService.isMockOffer(o))) {
            return offers;
        }
        Reference reference = fromHistory(origin, destination);
        if (reference == null) {
            reference = fromScan(cheapestPerDate);
        }
        if (reference == null) {
            return offers;
        }
        Reference ref = reference;
        return offers.stream()
                .map(o -> o.priceInsight() != null || PriceHistoryService.isMockOffer(o)
                        ? o
                        : o.withPriceInsight(ref.judge(o.price())))
                .toList();
    }

    private Reference fromHistory(String origin, String destination) {
        LocalDate today = LocalDate.now(clock);
        List<BigDecimal> prices = history
                .findByOriginAndDestinationAndDayGreaterThanEqualOrderByDayAsc(
                        origin, destination, today.minusDays(HISTORY_DAYS))
                .stream()
                .filter(e -> e.getDay().isBefore(today))
                .map(PriceHistoryEntry::getBestPrice)
                .toList();
        return prices.size() >= MIN_HISTORY_DAYS ? new Reference(prices, PriceInsight.HISTORY) : null;
    }

    private static Reference fromScan(List<FlightOffer> cheapestPerDate) {
        List<BigDecimal> prices = cheapestPerDate.stream()
                .filter(o -> !PriceHistoryService.isMockOffer(o))
                .map(FlightOffer::price)
                .toList();
        return prices.size() >= MIN_SCANNED_DATES ? new Reference(prices, PriceInsight.SCAN) : null;
    }

    /** Reference prices and the usual range (25th to 75th percentile) they give. */
    record Reference(List<BigDecimal> prices, String source) {

        BigDecimal low() {
            return percentile(0.25);
        }

        BigDecimal high() {
            return percentile(0.75);
        }

        /** Nearest-rank percentile. */
        private BigDecimal percentile(double p) {
            List<BigDecimal> sorted = prices.stream().sorted().toList();
            int rank = (int) Math.ceil(p * sorted.size());
            return sorted.get(Math.max(0, rank - 1));
        }

        PriceInsight judge(BigDecimal price) {
            BigDecimal low = low();
            BigDecimal high = high();
            // When the range collapses (all prices alike), equal means typical.
            String level = price.compareTo(low) <= 0 && price.compareTo(high) < 0 ? "low"
                    : price.compareTo(high) >= 0 && price.compareTo(low) > 0 ? "high"
                    : "typical";
            return new PriceInsight(level, low.setScale(0, RoundingMode.HALF_UP),
                    high.setScale(0, RoundingMode.HALF_UP), source, prices.size());
        }
    }
}
