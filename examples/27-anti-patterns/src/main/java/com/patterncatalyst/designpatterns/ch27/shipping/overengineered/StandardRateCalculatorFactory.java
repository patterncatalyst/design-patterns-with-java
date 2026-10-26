package com.patterncatalyst.designpatterns.ch27.shipping.overengineered;

/**
 * OVER-ENGINEERED EXHIBIT. The only {@link RateCalculatorFactory}
 * implementation this codebase has ever had, wrapped around a constructor
 * call that never varies. A reader has to open this interface, confirm it
 * has one implementation, then open this class to find the actual logic:
 * three hops to reach code that {@code new StandardRateCalculator()} would
 * have put on the same line as the call site.
 */
public final class StandardRateCalculatorFactory implements RateCalculatorFactory {

    @Override
    public RateCalculator createCalculator() {
        return new StandardRateCalculator();
    }
}
