package com.patterncatalyst.designpatterns.ch10.shipment;

/**
 * Every status a shipment can carry, across all three expressions in this
 * example. {@code LOST} is reachable only through
 * {@link StochasticShipmentMachine#advance()}; nothing in the deterministic
 * event vocabulary below produces it.
 */
public enum ShipmentStatus {
    CREATED,
    PACKED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED,
    RETURNED,
    LOST,
}
