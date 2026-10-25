package com.patterncatalyst.designpatterns.ch05.shipping;

/**
 * A single parcel manifested for carrier pickup: its tracking id and
 * declared weight in kilograms.
 */
public record Manifest(String trackingId, double weightKg) {

    public Manifest {
        if (trackingId == null || trackingId.isBlank()) {
            throw new IllegalArgumentException("trackingId must not be blank");
        }
        if (weightKg <= 0.0) {
            throw new IllegalArgumentException("weightKg must be positive: " + weightKg);
        }
    }
}
