package com.patterncatalyst.designpatterns.ch06.shipping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The ordered scan history of a single shipment. The compact constructor
 * runs {@link List#copyOf(java.util.Collection)} on whatever list a caller
 * passes in, which does two jobs in one call: it copies the caller's list so
 * a later mutation of the original cannot reach this history's state, and it
 * wraps the copy in an immutable list so {@link #events()} cannot be
 * mutated by a caller either. {@link #empty()} is the static-factory caching
 * idiom {@code List.of()} and {@code Optional.empty()} both use: every empty
 * history is the exact same shared instance, not a freshly allocated one.
 *
 * @param events the scans, oldest first
 */
public record TrackingHistory(List<TrackingEvent> events) {

    private static final TrackingHistory EMPTY = new TrackingHistory(List.of());

    public TrackingHistory {
        Objects.requireNonNull(events, "events");
        events = List.copyOf(events);
    }

    public static TrackingHistory empty() {
        return EMPTY;
    }

    /**
     * Returns a new history with {@code event} appended. The history this
     * method was called on is never modified; a {@code Shipment} can hand
     * out its current history freely, knowing no caller can use it to reach
     * into a later version of that shipment.
     */
    public TrackingHistory withEvent(TrackingEvent event) {
        Objects.requireNonNull(event, "event");
        List<TrackingEvent> appended = new ArrayList<>(events);
        appended.add(event);
        return new TrackingHistory(appended);
    }

    public Optional<TrackingEvent> latest() {
        return events.isEmpty() ? Optional.empty() : Optional.of(events.get(events.size() - 1));
    }
}
