package com.patterncatalyst.designpatterns.ch24.shipping;

import java.util.Objects;

/**
 * A shipment awaiting a rate quote. This chapter's fan-out requests a quote
 * for the same {@code Shipment} from every carrier concurrently.
 */
public record Shipment(String shipmentId, double weightKg) {

    public Shipment {
        Objects.requireNonNull(shipmentId, "shipmentId");
        if (shipmentId.isBlank()) {
            throw new IllegalArgumentException("shipmentId cannot be blank");
        }
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("weightKg cannot be negative: " + weightKg);
        }
    }
}
