package com.patterncatalyst.designpatterns.ch19.shipping.manifest;

import java.util.Objects;

/** One entry on a {@link ShipmentManifest}. */
public record Shipment(String trackingNumber, DestinationZone zone, double weightKg) {

    public Shipment {
        Objects.requireNonNull(trackingNumber, "trackingNumber");
        Objects.requireNonNull(zone, "zone");
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("weightKg cannot be negative: " + weightKg);
        }
    }
}
