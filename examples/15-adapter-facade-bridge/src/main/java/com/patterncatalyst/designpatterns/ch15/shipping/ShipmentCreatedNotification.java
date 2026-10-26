package com.patterncatalyst.designpatterns.ch15.shipping;

import java.util.Objects;

public final class ShipmentCreatedNotification extends Notification {

    private final String trackingNumber;

    public ShipmentCreatedNotification(DeliveryChannel channel, String trackingNumber) {
        super(channel);
        this.trackingNumber = Objects.requireNonNull(trackingNumber, "trackingNumber");
    }

    @Override
    protected String formatMessage() {
        return "Your shipment %s has been created.".formatted(trackingNumber);
    }
}
