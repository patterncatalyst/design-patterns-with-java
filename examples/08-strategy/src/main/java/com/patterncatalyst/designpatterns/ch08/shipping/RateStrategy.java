package com.patterncatalyst.designpatterns.ch08.shipping;

/**
 * The Strategy contract: given a shipment, produce a quote. This single
 * abstract method is deliberately the only thing a caller such as
 * {@link ShippingRateCalculator} depends on. Every algorithm in this example,
 * whether it is a named class in the {@code named} package, a lambda built
 * by {@link RateStrategies}, or a {@link Carrier} enum constant,
 * satisfies this same interface, so a caller can hold any of them in one
 * field and never be rewritten when the algorithm changes underneath it.
 */
@FunctionalInterface
public interface RateStrategy {

    RateQuote quote(Shipment shipment);
}
