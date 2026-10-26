package com.patterncatalyst.designpatterns.ch15.shipping;

import java.util.Objects;

/**
 * A parcel moving through the GlobalCourier integration: a weight, a
 * destination zone, and a declared value a customs declaration can quote
 * from. Validation lives in the compact constructor, so once a
 * {@code Shipment} exists, every technique downstream, the Adapter, the
 * Facade, and the Bridge notifications it triggers, can trust its fields
 * without re-checking them.
 *
 * @param weightKg the parcel's weight in kilograms; never negative
 * @param zone the destination zone
 * @param declaredValueCents the declared value in integer cents; never
 *     negative
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
