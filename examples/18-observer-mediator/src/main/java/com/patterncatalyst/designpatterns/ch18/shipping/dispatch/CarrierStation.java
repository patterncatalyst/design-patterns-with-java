package com.patterncatalyst.designpatterns.ch18.shipping.dispatch;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A carrier confirms a pickup, reports a shipment out for delivery, and
 * occasionally reports a delivery exception, raising one event per method
 * without ever calling {@link CustomerNotificationStation} directly.
 */
public final class CarrierStation extends DispatchColleague {

    private final AtomicInteger pickupCount = new AtomicInteger();

    public void confirmPickup(String orderId) {
        Objects.requireNonNull(orderId, "orderId");
        pickupCount.incrementAndGet();
        raise(DispatchEvent.PICKED_UP_BY_CARRIER, orderId);
    }

    public void markOutForDelivery(String orderId) {
        Objects.requireNonNull(orderId, "orderId");
        raise(DispatchEvent.OUT_FOR_DELIVERY, orderId);
    }

    public void reportDeliveryException(String orderId) {
        Objects.requireNonNull(orderId, "orderId");
        raise(DispatchEvent.DELIVERY_EXCEPTION, orderId);
    }

    public int pickupCount() {
        return pickupCount.get();
    }
}
