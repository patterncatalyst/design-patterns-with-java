package com.patterncatalyst.designpatterns.ch06.shipping;

import java.util.Objects;

/**
 * A shipment moving between two points, carrying one {@link Manifest} and
 * its {@link TrackingHistory} so far. Every field is itself immutable, so a
 * {@code Shipment} reference handed to one caller can never be changed by
 * another; {@link #withEvent(TrackingEvent)} is the only way to move a
 * shipment forward, and it always returns a brand-new value rather than
 * touching the one it was called on.
 *
 * @param trackingId  a stable, externally visible tracking id; never blank
 * @param origin      the shipment's origin, in plain text
 * @param destination the shipment's destination, in plain text
 * @param manifest    the bill of lading this shipment carries
 * @param history     every scan recorded against this shipment so far
 */
public record Shipment(String trackingId, String origin, String destination, Manifest manifest, TrackingHistory history) {

    public Shipment {
        Objects.requireNonNull(trackingId, "trackingId");
        if (trackingId.isBlank()) {
            throw new IllegalArgumentException("trackingId cannot be blank");
        }
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(history, "history");
    }

    /**
     * The static factory a caller actually reaches for: a newly created
     * shipment always starts from the one shared {@link TrackingHistory#empty()}
     * instance, so a caller never has to know how an empty history is built.
     */
    public static Shipment created(String trackingId, String origin, String destination, Manifest manifest) {
        return new Shipment(trackingId, origin, destination, manifest, TrackingHistory.empty());
    }

    public Shipment withEvent(TrackingEvent event) {
        return new Shipment(trackingId, origin, destination, manifest, history.withEvent(event));
    }
}
