package com.patterncatalyst.designpatterns.ch12.shipping;

import java.util.Objects;

/**
 * A shipment leaving one regional hub. {@code region} is the fact that
 * decides which {@link ShippingToolkit} must build this shipment's label,
 * rate, and customs form, all three from the same factory call.
 */
public record Shipment(
        String destinationCountry,
        double weightKg,
        long declaredValueCents,
        Region region) {

    public Shipment {
        Objects.requireNonNull(destinationCountry, "destinationCountry");
        Objects.requireNonNull(region, "region");
        if (weightKg <= 0.0) {
            throw new IllegalArgumentException("weightKg must be positive: " + weightKg);
        }
        if (declaredValueCents < 0) {
            throw new IllegalArgumentException("declaredValueCents cannot be negative: " + declaredValueCents);
        }
    }
}
