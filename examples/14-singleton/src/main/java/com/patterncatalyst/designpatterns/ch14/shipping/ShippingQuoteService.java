package com.patterncatalyst.designpatterns.ch14.shipping;

import java.util.Objects;

/**
 * The dependency-injection alternative to {@link WarehousePricingJob}. This
 * class needs exactly the same capability, a base rate for a carrier code,
 * but receives it as a {@link RateLookup} passed through the constructor
 * instead of reaching into a global singleton. The dependency is stated
 * once, in the one place a reader is guaranteed to look.
 *
 * <p>Because the constructor accepts any {@link RateLookup}, production code
 * can wire in {@link CarrierRateTable#INSTANCE} or
 * {@link HolderCarrierRateTable#getInstance()} and a test can wire in a
 * Mockito double, with no change to this class either way.
 */
public final class ShippingQuoteService {

    private final RateLookup rateLookup;

    public ShippingQuoteService(RateLookup rateLookup) {
        this.rateLookup = Objects.requireNonNull(rateLookup, "rateLookup");
    }

    public Money quote(String carrierCode, double weightKg) {
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("weightKg cannot be negative: " + weightKg);
        }
        Money baseRate = rateLookup.baseRateFor(carrierCode);
        return Money.ofCents(Math.round(baseRate.cents() * weightKg));
    }
}
