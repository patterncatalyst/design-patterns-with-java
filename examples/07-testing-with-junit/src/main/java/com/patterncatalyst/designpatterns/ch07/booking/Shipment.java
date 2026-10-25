package com.patterncatalyst.designpatterns.ch07.booking;

import java.util.Objects;

/**
 * A shipment offered for booking. The compact constructor enforces only the
 * structural invariants: a shipment must name a cargo class and a
 * destination. It deliberately does not reject a non-positive weight or a
 * non-positive declared value; whether those are acceptable is a business
 * rule, not a structural one, and {@link ShipmentValidator} is where that
 * rule lives and where this chapter's tests can construct a well-formed
 * record that is still an invalid shipment.
 */
public record Shipment(double weightKg, CargoClass cargoClass, String destinationCode, long declaredValueCents) {

    public Shipment {
        Objects.requireNonNull(cargoClass, "cargoClass");
        if (destinationCode == null || destinationCode.isBlank()) {
            throw new IllegalArgumentException("destinationCode must not be blank");
        }
    }
}
