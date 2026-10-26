package com.patterncatalyst.designpatterns.ch27.shipping;

/**
 * The entire configuration surface a shipment-pricing calculation needs: a
 * fuel surcharge multiplier and a flat handling fee, both known at
 * construction time and immune to being changed out from under a caller
 * mid-calculation. Nothing here varies polymorphically between regions, so
 * nothing here needs an interface; a plain, copyable record is the right
 * level of abstraction for data with this shape, the same judgment that
 * applies to value types generally.
 *
 * @param surchargeMultiplier the fuel surcharge multiplier; {@code 1.0}
 *     means no surcharge
 * @param handlingFeeCents a flat handling fee, in cents, added after the
 *     surcharge is applied
 */
public record RateSettings(double surchargeMultiplier, long handlingFeeCents) {

    public RateSettings {
        if (surchargeMultiplier <= 0.0) {
            throw new IllegalArgumentException(
                    "surchargeMultiplier must be positive: " + surchargeMultiplier);
        }
        if (handlingFeeCents < 0) {
            throw new IllegalArgumentException("handlingFeeCents cannot be negative: " + handlingFeeCents);
        }
    }
}
