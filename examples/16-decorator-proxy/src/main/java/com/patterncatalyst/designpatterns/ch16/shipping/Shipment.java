package com.patterncatalyst.designpatterns.ch16.shipping;

import java.util.Objects;

/**
 * A shipment awaiting a rate quote. Equality is structural, which is what
 * lets {@code CachingRateQuoter} use a {@code Shipment} as a cache key
 * without writing hashCode/equals by hand.
 */
public record Shipment(double weightKg, DestinationZone zone, long declaredValueCents) {

    public Shipment {
        Objects.requireNonNull(zone, "zone");
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("weightKg cannot be negative: " + weightKg);
        }
        if (declaredValueCents < 0) {
            throw new IllegalArgumentException("declaredValueCents cannot be negative: " + declaredValueCents);
        }
    }
}
