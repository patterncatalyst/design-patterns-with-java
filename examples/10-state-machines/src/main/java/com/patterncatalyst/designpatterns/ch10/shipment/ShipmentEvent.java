package com.patterncatalyst.designpatterns.ch10.shipment;

/**
 * Every event that can be asked of a shipment. {@code ADVANCE} is not a
 * caller-issued event; it exists only so
 * {@link StochasticShipmentMachine#advance()} can report a rejection the
 * same way every other event does, through
 * {@link IllegalTransitionException}, when it is called outside
 * {@code IN_TRANSIT}.
 */
public enum ShipmentEvent {
    PACK,
    SHIP,
    DELIVER,
    CANCEL,
    RETURN_TO_SENDER,
    ADVANCE,
}
