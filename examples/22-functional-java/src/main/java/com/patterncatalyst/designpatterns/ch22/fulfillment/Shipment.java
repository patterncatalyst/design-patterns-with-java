package com.patterncatalyst.designpatterns.ch22.fulfillment;

import java.util.Objects;

/**
 * One order in the nightly fulfillment batch: a weight, a destination zone,
 * a packaging tier, and a declared value used for the retail validation
 * rule in {@link FulfillmentPipeline}.
 */
public record Shipment(
        String orderId,
        double weightKg,
        DestinationZone zone,
        PackagingTier tier,
        long declaredValueCents) {

    public Shipment {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(tier, "tier");
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("weightKg cannot be negative: " + weightKg);
        }
        if (declaredValueCents < 0L) {
            throw new IllegalArgumentException("declaredValueCents cannot be negative: " + declaredValueCents);
        }
    }
}
