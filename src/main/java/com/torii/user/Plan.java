package com.torii.user;

import com.torii.model.SearchPrecision;

import java.util.EnumSet;
import java.util.Set;

/**
 * Torii plans and their monthly lookup quota. Has to match what the frontend
 * /planes page promises.
 *
 * <p>Keep in mind one SEARCH uses several LOOKUPS (one per date pair explored). The
 * estimator in the form shows the cost before searching. Each plan also caps how
 * fine-grained a search can be: FREE only scans every 3 days (FAST).
 */
public enum Plan {
    FREE(30, EnumSet.of(SearchPrecision.FAST)),
    PRO(500, EnumSet.of(SearchPrecision.FAST, SearchPrecision.BALANCED)),
    BUSINESS(null, EnumSet.allOf(SearchPrecision.class)); // null = no limit

    private final Integer monthlyQueries;
    private final Set<SearchPrecision> precisions;

    Plan(Integer monthlyQueries, Set<SearchPrecision> precisions) {
        this.monthlyQueries = monthlyQueries;
        this.precisions = precisions;
    }

    /** Whether searches at this precision are included in the plan. */
    public boolean allows(SearchPrecision precision) {
        return precisions.contains(precision);
    }

    /** The finest precision the plan includes, used when a search doesn't ask for one. */
    public SearchPrecision bestPrecision() {
        return allows(SearchPrecision.EXHAUSTIVE) ? SearchPrecision.EXHAUSTIVE
                : allows(SearchPrecision.BALANCED) ? SearchPrecision.BALANCED
                : SearchPrecision.FAST;
    }

    /** Monthly lookup quota, or null if unlimited. */
    public Integer monthlyQueries() {
        return monthlyQueries;
    }

    /** Lenient with old data: an unknown plan in the DB is treated as FREE. */
    public static Plan fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return FREE;
        }
    }
}
