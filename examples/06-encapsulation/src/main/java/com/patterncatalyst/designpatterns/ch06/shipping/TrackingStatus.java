package com.patterncatalyst.designpatterns.ch06.shipping;

/**
 * The closed set of states a {@link TrackingEvent} can report. A shipment
 * moves through these in order, except {@link #EXCEPTION}, which can occur
 * after any of the others.
 */
public enum TrackingStatus {
    CREATED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    EXCEPTION
}
