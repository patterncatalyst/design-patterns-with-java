package com.patterncatalyst.designpatterns.ch18.shipping.tracking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Observer subject. {@code ShipmentTracker} owns one shipment's current
 * {@link ShipmentStatus} and fans every change out to whoever is currently
 * subscribed. It never imports, extends, or even knows the name of a
 * concrete listener; the only contract it depends on is
 * {@link ShipmentStatusListener}, which any lambda, method reference, or
 * named class can satisfy.
 */
public final class ShipmentTracker {

    private final String trackingId;
    private final Map<Long, ShipmentStatusListener> listeners = new LinkedHashMap<>();
    private final AtomicLong nextSubscriptionId = new AtomicLong();
    private ShipmentStatus status = ShipmentStatus.CREATED;

    public ShipmentTracker(String trackingId) {
        this.trackingId = Objects.requireNonNull(trackingId, "trackingId");
    }

    /**
     * Subscribes a listener and returns a handle that unsubscribes it on
     * {@link Subscription#close()}. Rejects a subscribe once the shipment
     * has already reached a terminal status, since there is no future
     * status change left for a new listener to ever hear about.
     */
    public Subscription subscribe(ShipmentStatusListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (status.isTerminal()) {
            throw new IllegalStateException(
                    "cannot subscribe to shipment " + trackingId + ": already reached terminal status " + status);
        }
        long id = nextSubscriptionId.getAndIncrement();
        listeners.put(id, listener);
        return () -> listeners.remove(id);
    }

    /**
     * Moves the tracked status forward and notifies every currently
     * subscribed listener, in subscription order, with the tracking ID, the
     * old status, and the new one.
     *
     * <p>Notification runs over a snapshot copy of the listener map rather
     * than the live one. A listener is free to unsubscribe itself, or
     * trigger an unsubscribe of a different listener, from inside its own
     * callback; without the snapshot, that would mutate the very map this
     * method is iterating and raise a {@link java.util.ConcurrentModificationException}.
     */
    public void advanceStatus(ShipmentStatus newStatus) {
        Objects.requireNonNull(newStatus, "newStatus");
        if (status.isTerminal()) {
            throw new IllegalStateException(
                    "cannot advance shipment " + trackingId + " past terminal status " + status);
        }
        ShipmentStatus oldStatus = status;
        status = newStatus;
        List<ShipmentStatusListener> snapshot = new ArrayList<>(listeners.values());
        for (ShipmentStatusListener listener : snapshot) {
            listener.onStatusChanged(trackingId, oldStatus, newStatus);
        }
    }

    public ShipmentStatus currentStatus() {
        return status;
    }

    public String trackingId() {
        return trackingId;
    }

    public int subscriberCount() {
        return listeners.size();
    }
}
