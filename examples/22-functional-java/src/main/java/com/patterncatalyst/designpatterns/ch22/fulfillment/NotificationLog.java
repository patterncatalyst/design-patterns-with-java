package com.patterncatalyst.designpatterns.ch22.fulfillment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Records which channels a shipment was notified through. Its two instance
 * methods are the receivers {@link NotificationCommand} method references
 * bind to; neither method knows it is being used as a Command, it is just an
 * ordinary method that happens to match the one-argument, no-return shape
 * {@link NotificationCommand} declares.
 */
public final class NotificationLog {

    private final List<String> entries = new ArrayList<>();

    public void recordEmail(Shipment shipment) {
        Objects.requireNonNull(shipment, "shipment");
        entries.add("email:" + shipment.orderId());
    }

    public void recordSms(Shipment shipment) {
        Objects.requireNonNull(shipment, "shipment");
        entries.add("sms:" + shipment.orderId());
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }
}
