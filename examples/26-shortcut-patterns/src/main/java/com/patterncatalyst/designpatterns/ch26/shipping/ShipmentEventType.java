package com.patterncatalyst.designpatterns.ch26.shipping;

/**
 * The closed set of shipment lifecycle events this codebase owns and
 * compiles against directly. Adding a seventh constant here without adding
 * a matching case to {@link EnumDispatchRouter}'s switch expression is a
 * compile error, not a runtime surprise.
 */
public enum ShipmentEventType {
    CREATED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    EXCEPTION
}
