package com.patterncatalyst.designpatterns.ch27.shipping.overengineered;

/**
 * OVER-ENGINEERED EXHIBIT. A second singleton, built for the sole purpose
 * of handing out the one {@link RateCalculatorFactory} this program will
 * ever construct. A caller reaches a concrete {@link StandardRateCalculator}
 * through {@code RateCalculatorFactoryProvider.INSTANCE.factory().createCalculator()},
 * four symbols and two singletons deep, to get the exact same object a
 * direct {@code new StandardRateCalculator()} would hand back in one line.
 * This is a factory of factories: speculative generality compounding with
 * Singleton's hidden-dependency cost, each one hiding the other's problem
 * well enough that a reviewer skimming either class alone can miss both.
 */
public enum RateCalculatorFactoryProvider {
    INSTANCE;

    private final RateCalculatorFactory factory = new StandardRateCalculatorFactory();

    public RateCalculatorFactory factory() {
        return factory;
    }
}
