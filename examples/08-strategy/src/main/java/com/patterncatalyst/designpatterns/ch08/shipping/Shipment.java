package com.patterncatalyst.designpatterns.ch08.shipping;

import java.util.Objects;

/**
 * A parcel at the moment checkout asks "what will this cost to ship?" Every
 * {@link RateStrategy} in this example reads the same three fields; none of
 * them ever mutate a {@code Shipment} or reach outside it for more context.
 *
 * @param weightKg             the parcel's weight in kilograms; never negative
 * @param zone                 the destination's distance tier
 * @param declaredValueCents   the buyer's declared value, in integer cents, used
 *                             only by strategies that price insurance
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
