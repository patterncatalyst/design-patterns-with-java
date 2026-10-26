package com.patterncatalyst.designpatterns.ch27.shipping.overengineered;

import com.patterncatalyst.designpatterns.ch27.shipping.Money;

/**
 * OVER-ENGINEERED EXHIBIT. An abstraction with exactly one concrete
 * implementation, {@link StandardRateCalculator}, and no second one planned
 * anywhere in this codebase or a credible one on the horizon: speculative
 * generality, an interface built for a variation that has not arrived.
 */
public interface RateCalculator {

    Money calculate(Money baseCost);
}
