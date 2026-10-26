package com.patterncatalyst.designpatterns.ch33.shipping;

/**
 * The varying algorithm this final project composes with the others: how a
 * shipment's quote is computed. {@link ShipmentBuilder} resolves one of
 * these per carrier code when a shipment is opened, {@link Shipment} holds
 * the resolved instance for the rest of its life, and
 * {@link SwitchRateStrategyCommand} is the one operation allowed to replace
 * it. Every implementation is interchangeable from a caller's point of
 * view: nothing downstream of this interface ever names a concrete type.
 */
@FunctionalInterface
public interface RateStrategy {

    Money quote(Shipment shipment);
}
