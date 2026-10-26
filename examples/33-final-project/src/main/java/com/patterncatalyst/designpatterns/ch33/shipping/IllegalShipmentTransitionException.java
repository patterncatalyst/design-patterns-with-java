package com.patterncatalyst.designpatterns.ch33.shipping;

/**
 * Thrown when {@link Shipment#advance(ShipmentEvent)} is asked to apply an
 * event that does not apply to the shipment's current status, carrying both
 * values so the message names the exact rejected move.
 */
public final class IllegalShipmentTransitionException extends RuntimeException {

    public IllegalShipmentTransitionException(ShipmentStatus from, ShipmentEvent event) {
        super("cannot apply event '%s' to a shipment in status '%s'".formatted(event, from));
    }
}
