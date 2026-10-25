package com.patterncatalyst.designpatterns.ch08.shipping;

import java.util.Objects;

/**
 * The caller that uses a {@link RateStrategy} without knowing which one it
 * holds. {@code ShippingRateCalculator} owns exactly one strategy at a time,
 * supplied through the constructor and reassignable later with
 * {@link #useStrategy(RateStrategy)}; nothing about this class changes
 * whether that strategy is a named concrete class, a lambda,
 * or a {@link Carrier} enum constant.
 */
public final class ShippingRateCalculator {

    private RateStrategy strategy;

    public ShippingRateCalculator(RateStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public void useStrategy(RateStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public RateQuote quote(Shipment shipment) {
        return strategy.quote(shipment);
    }
}
