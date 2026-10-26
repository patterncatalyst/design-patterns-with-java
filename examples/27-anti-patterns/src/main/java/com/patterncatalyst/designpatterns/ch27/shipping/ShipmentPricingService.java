package com.patterncatalyst.designpatterns.ch27.shipping;

import java.util.Objects;

/**
 * The simplified, appropriate form of the pricing design the
 * {@code overengineered} package stacks five types and two singletons to
 * express. {@code overengineered.ShipmentOperationsManager} resolves a rate
 * calculator through a singleton factory provider, validates its input,
 * writes an audit log entry, and formats a receipt, four responsibilities
 * with nothing in common except that nothing else owned them yet. This
 * class keeps exactly the one responsibility its name promises: compute a
 * total cost from a base cost and a fixed {@link RateSettings}.
 *
 * <p>A region's entire pricing configuration is one immutable value,
 * injected once at construction. Two instances configured differently never
 * interfere with each other, because neither one reads from anything
 * shared; see {@code ShipmentPricingServiceTest} for the test that proves
 * exactly that independence.
 */
public final class ShipmentPricingService {

    private final RateSettings settings;

    public ShipmentPricingService(RateSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public PricingResult price(Money baseCost) {
        Objects.requireNonNull(baseCost, "baseCost");
        if (baseCost.cents() <= 0) {
            throw new IllegalArgumentException("baseCost must be positive: " + baseCost);
        }

        long surchargedCents = Math.round(baseCost.cents() * settings.surchargeMultiplier());
        long totalCents = surchargedCents + settings.handlingFeeCents();
        boolean surchargeApplied = settings.surchargeMultiplier() != 1.0;
        return new PricingResult(Money.ofCents(totalCents), surchargeApplied);
    }
}
