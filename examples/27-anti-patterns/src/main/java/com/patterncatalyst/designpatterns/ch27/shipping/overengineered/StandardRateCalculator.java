package com.patterncatalyst.designpatterns.ch27.shipping.overengineered;

import com.patterncatalyst.designpatterns.ch27.shipping.Money;

/**
 * OVER-ENGINEERED EXHIBIT. The only {@link RateCalculator} implementation
 * this codebase has ever had. It reads
 * {@link ShippingSurchargeRegistry}'s surcharge multiplier fresh on every
 * call rather than capturing it once at construction, which is the specific
 * design choice that turns a convenient global accessor into a correctness
 * bug waiting on a test to expose it.
 */
public final class StandardRateCalculator implements RateCalculator {

    @Override
    public Money calculate(Money baseCost) {
        double multiplier = ShippingSurchargeRegistry.INSTANCE.surchargeMultiplier();
        long cents = Math.round(baseCost.cents() * multiplier);
        return Money.ofCents(cents);
    }
}
