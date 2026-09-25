package com.torii.user;

/**
 * Torii plans and their monthly lookup quota. Has to match what the frontend
 * /planes page promises.
 *
 * <p>Keep in mind one SEARCH uses several LOOKUPS (one per date pair explored). The
 * estimator in the form shows the cost before searching.
 */
public enum Plan {
    FREE(30),
    PRO(500),
    BUSINESS(null); // null = no limit

    private final Integer monthlyQueries;

    Plan(Integer monthlyQueries) {
        this.monthlyQueries = monthlyQueries;
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
