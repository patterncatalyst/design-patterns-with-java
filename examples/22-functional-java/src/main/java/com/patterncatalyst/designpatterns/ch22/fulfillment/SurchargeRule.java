package com.patterncatalyst.designpatterns.ch22.fulfillment;

/**
 * A single-method contract for pricing a surcharge on a {@link Shipment}.
 * {@link PackagingTier} satisfies it with constant-specific method bodies on
 * a closed set of three tiers; {@link PromotionalSurcharges} satisfies the
 * identical contract with one-expression lambdas for surcharges that come
 * and go with a sales calendar rather than with the physical package. A
 * variable typed {@code SurchargeRule} cannot tell which of the two produced
 * the value it holds.
 */
@FunctionalInterface
public interface SurchargeRule {
    long surchargeCents(Shipment shipment);
}
