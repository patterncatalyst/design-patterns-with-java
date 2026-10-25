package com.patterncatalyst.designpatterns.ch06.shipping;

import java.time.Instant;
import java.util.Objects;

/**
 * One scan of a shipment at a point in its journey. {@code occurredAt} is an
 * {@link Instant}, a type with no setters and no mutating methods at all, so
 * nothing in this record needs to defend against a caller reaching back in
 * and changing a timestamp after the fact. A mutable type like
 * {@link java.util.Date} would need a defensive copy on the way in and on
 * the way out to make the same guarantee; an {@code Instant} field needs
 * none.
 *
 * @param occurredAt when the event happened
 * @param location   where the scan happened; never blank
 * @param status     the shipment's state as of this scan
 */
public record TrackingEvent(Instant occurredAt, String location, TrackingStatus status) {

    public TrackingEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(status, "status");
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("location cannot be blank");
        }
    }
}
