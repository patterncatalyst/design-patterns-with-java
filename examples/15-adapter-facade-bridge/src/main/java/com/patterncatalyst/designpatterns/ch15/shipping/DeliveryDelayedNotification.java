package com.patterncatalyst.designpatterns.ch15.shipping;

import java.time.Duration;
import java.util.Objects;

public final class DeliveryDelayedNotification extends Notification {

    private final String trackingNumber;
    private final Duration delay;

    public DeliveryDelayedNotification(DeliveryChannel channel, String trackingNumber, Duration delay) {
        super(channel);
        this.trackingNumber = Objects.requireNonNull(trackingNumber, "trackingNumber");
        this.delay = Objects.requireNonNull(delay, "delay");
    }

    @Override
    protected String formatMessage() {
        return "Shipment %s is delayed by %d hours.".formatted(trackingNumber, delay.toHours());
    }
}
