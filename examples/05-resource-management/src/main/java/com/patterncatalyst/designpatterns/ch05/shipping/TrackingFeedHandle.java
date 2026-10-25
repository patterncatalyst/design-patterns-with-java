package com.patterncatalyst.designpatterns.ch05.shipping;

import java.util.Objects;

/**
 * A live subscription to a carrier's tracking feed. Unlike a
 * {@link CarrierConnection} opened and closed around a single manifest
 * submission, a tracking feed subscription is meant to stay open across
 * many unrelated calls for as long as a shipment is in transit. This chapter
 * does not wrap it in a per-call try-with-resources block; see
 * {@link TrackingFeedRegistry} for the ownership model it uses instead.
 */
public final class TrackingFeedHandle implements AutoCloseable {

    private final String carrierEndpoint;
    private boolean subscribed;

    private TrackingFeedHandle(String carrierEndpoint) {
        this.carrierEndpoint = carrierEndpoint;
        this.subscribed = true;
    }

    public static TrackingFeedHandle subscribe(String carrierEndpoint) {
        Objects.requireNonNull(carrierEndpoint, "carrierEndpoint");
        return new TrackingFeedHandle(carrierEndpoint);
    }

    public TrackingUpdate poll(String trackingId) {
        if (!subscribed) {
            throw new IllegalStateException(
                    "tracking feed for " + carrierEndpoint + " is no longer subscribed");
        }
        Objects.requireNonNull(trackingId, "trackingId");
        return new TrackingUpdate(trackingId, "IN_TRANSIT");
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    @Override
    public void close() {
        subscribed = false;
    }
}
