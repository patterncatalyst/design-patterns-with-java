package com.patterncatalyst.designpatterns.ch28.shipping;

import java.util.Objects;

/**
 * A shipment to be quoted: a tracking ID, a weight in kilograms, and a
 * declared value in cents.
 */
public record Shipment(String trackingId, double weightKg, long declaredValueCents) {

    public Shipment {
        Objects.requireNonNull(trackingId, "trackingId");
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("weightKg cannot be negative: " + weightKg);
        }
    }
}
