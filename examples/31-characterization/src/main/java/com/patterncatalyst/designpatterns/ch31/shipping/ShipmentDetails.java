package com.patterncatalyst.designpatterns.ch31.shipping;

/**
 * Introduce Parameter Object: the six positional arguments the legacy
 * calculator takes on every call, {@code carrierCode}, {@code originZone},
 * {@code destinationZone}, {@code weightKg}, {@code isResidential},
 * {@code declaredValueCents}, collapsed into one value a caller builds once
 * and passes around instead of six arguments whose order a reader has to
 * remember.
 */
public record ShipmentDetails(
        String carrierCode,
        String originZone,
        String destinationZone,
        double weightKg,
        boolean isResidential,
        long declaredValueCents) {
}
