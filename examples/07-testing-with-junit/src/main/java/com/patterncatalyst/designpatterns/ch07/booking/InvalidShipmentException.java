package com.patterncatalyst.designpatterns.ch07.booking;

/**
 * Thrown by {@link ShipmentValidator} when a structurally well-formed
 * {@link Shipment} still violates a business rule: a non-positive weight, a
 * declared value outside the insurable range, or a hazmat shipment over the
 * unescorted weight ceiling.
 */
public final class InvalidShipmentException extends RuntimeException {

    public InvalidShipmentException(String reason) {
        super(reason);
    }
}
