package com.patterncatalyst.designpatterns.ch10.shipment;

/**
 * Thrown by every machine in this example the same way: a status cannot
 * accept an event. Sharing one exception type across the enum machine, the
 * sealed-interface machine, and the stochastic machine is what lets a test
 * assert "this event is illegal from this status" without caring which
 * machine rejected it.
 */
public final class IllegalTransitionException extends RuntimeException {

    public IllegalTransitionException(ShipmentStatus from, ShipmentEvent event) {
        super("cannot apply event '%s' to a shipment in status '%s'".formatted(event, from));
    }
}
