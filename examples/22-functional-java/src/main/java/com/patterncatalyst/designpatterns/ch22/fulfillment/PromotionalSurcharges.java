package com.patterncatalyst.designpatterns.ch22.fulfillment;

import java.util.Objects;

/**
 * Surcharges tied to a sales calendar rather than to a package's physical
 * tier, satisfying the identical {@link SurchargeRule} contract
 * {@link PackagingTier}'s constants implement, but as one-expression lambdas
 * instead of enum constants. A promotional surcharge is built once at a call
 * site for a single peak-season campaign and discarded afterward; giving it
 * a named class and a file would exist purely to hold a constructor nobody
 * reuses once the campaign ends.
 */
public final class PromotionalSurcharges {

    private PromotionalSurcharges() {
    }

    public static SurchargeRule holidayPeakSurcharge(long centsPerKg) {
        return shipment -> Math.round(shipment.weightKg() * centsPerKg);
    }

    public static SurchargeRule flatCampaignSurcharge(long flatCents) {
        return shipment -> flatCents;
    }

    /**
     * Combines two surcharge rules into a third one that adds both, the same
     * composition a lambda value gets for free that an enum constant cannot
     * offer on its own: a {@link PackagingTier} constant's body is fixed at
     * compile time, but two {@code SurchargeRule} values, from any source,
     * combine into a new value with no new type declared anywhere.
     */
    public static SurchargeRule combined(SurchargeRule first, SurchargeRule second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return shipment -> first.surchargeCents(shipment) + second.surchargeCents(shipment);
    }
}
