package com.torii.model;

/**
 * Search precision: how many days the departure date slides forward on each step
 * while exploring the holiday window.
 *
 * <p>This is the knob between <b>cost</b> (number of calls to the data source) and
 * <b>coverage</b> (chance of finding the absolute best offer). Skipping days means
 * fewer calls, but the exact cheapest day might be skipped too.
 *
 * <p>It also maps to the pricing plans:
 * <ul>
 *   <li>{@link #FAST}: free tier. Quick and cheap, good but not optimal.</li>
 *   <li>{@link #BALANCED}: middle ground.</li>
 *   <li>{@link #EXHAUSTIVE}: paid tier. Checks every single day, full coverage.</li>
 * </ul>
 */
public enum SearchPrecision {

    FAST(3),
    BALANCED(2),
    EXHAUSTIVE(1);

    private final int dayStep;

    SearchPrecision(int dayStep) {
        this.dayStep = dayStep;
    }

    /** Days the departure date moves forward on each iteration. */
    public int dayStep() {
        return dayStep;
    }
}
