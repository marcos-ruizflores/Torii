package com.torii.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One direction of a round trip (outbound or return), as shown in the itinerary
 * detail: when it leaves and lands (local times at each airport), how long it takes,
 * who flies it and where it stops.
 *
 * <p>Any field can be {@code null} when the source doesn't send it. Only the
 * layovers are known, not each flight segment, so the UI spaces the stops evenly.
 */
public record FlightLeg(
        String airline,
        LocalDateTime departure,
        LocalDateTime arrival,
        Integer durationMinutes,
        List<Layover> layovers
) {
    /** A stop on the way: airport and how long you wait there. */
    public record Layover(String airport, Integer minutes) {}
}
