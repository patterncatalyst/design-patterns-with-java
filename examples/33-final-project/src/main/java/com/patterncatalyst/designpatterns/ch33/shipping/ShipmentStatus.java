package com.patterncatalyst.designpatterns.ch33.shipping;

/**
 * A shipment's lifecycle status. {@link Shipment} is the single context
 * both the State machine and the Observer subject operate against: every
 * legal move between these five values is what {@link Shipment#advance}
 * enforces, and every move is what a subscribed {@link ShipmentStatusListener}
 * hears about.
 */
public enum ShipmentStatus {
    CREATED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED,
}
