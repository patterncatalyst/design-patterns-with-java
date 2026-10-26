package com.patterncatalyst.designpatterns.ch33.shipping;

/**
 * An event an operator or an {@link AdvanceShipmentCommand} submits to
 * {@link Shipment#advance(ShipmentEvent)}. Whether a given event is legal
 * from the shipment's current {@link ShipmentStatus} is decided entirely by
 * the transition table {@code Shipment} owns, not by this enum.
 */
public enum ShipmentEvent {
    PICK_UP,
    DISPATCH,
    DELIVER,
    CANCEL,
}
