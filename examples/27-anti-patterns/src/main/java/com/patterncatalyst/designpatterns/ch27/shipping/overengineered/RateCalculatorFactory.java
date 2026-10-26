package com.patterncatalyst.designpatterns.ch27.shipping.overengineered;

/**
 * OVER-ENGINEERED EXHIBIT. A second layer of speculative generality: a
 * factory abstraction for choosing which {@link RateCalculator} to build,
 * when exactly one concrete type, {@link StandardRateCalculator}, has ever
 * existed to choose between. Factory Method earns its place when choosing a
 * concrete type is itself a real decision, resolved by a config value, a
 * runtime flag, or a registered family of related types. Nothing about
 * {@link StandardRateCalculatorFactory#createCalculator()} ever varies.
 */
public interface RateCalculatorFactory {

    RateCalculator createCalculator();
}
