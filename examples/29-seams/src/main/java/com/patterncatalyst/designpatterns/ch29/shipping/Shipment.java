package com.patterncatalyst.designpatterns.ch29.shipping;

import java.time.Instant;
import java.util.Objects;

/**
 * A shipment with a tracking id, a promised delivery instant, and a weight
 * in kilograms. Both the legacy and refactored services in this chapter
 * share this one value type.
 */
public record Shipment(String id, Instant promisedDelivery, double weightKg) {

    public Shipment {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        Objects.requireNonNull(promisedDelivery, "promisedDelivery");
        if (weightKg <= 0.0) {
            throw new IllegalArgumentException("weightKg must be positive: " + weightKg);
        }
    }
}
