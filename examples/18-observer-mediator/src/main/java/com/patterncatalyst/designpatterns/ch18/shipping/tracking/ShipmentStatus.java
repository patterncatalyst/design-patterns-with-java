package com.patterncatalyst.designpatterns.ch18.shipping.tracking;

/**
 * A shipment's lifecycle from creation to delivery. {@link #DELIVERED} and
 * {@link #EXCEPTION} are terminal: once a shipment reaches either one, no
 * further status change, and so no further notification, will ever happen.
 */
public enum ShipmentStatus {
    CREATED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    EXCEPTION;

    public boolean isTerminal() {
        return this == DELIVERED || this == EXCEPTION;
    }
}
