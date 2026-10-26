package com.patterncatalyst.designpatterns.ch32.accessorials;

import java.util.Objects;

/**
 * The input both the legacy and the modern accessorial fee calculator
 * price. The compact constructor enforces only the structural invariants
 * (a non-blank shipment ID and carrier code, a non-negative declared
 * value, a positive weight); whether a given combination of flags should
 * trigger a fee is a business rule, not a structural one, and belongs to
 * {@link AccessorialFeeCalculator}, not to this record.
 */
public record ShipmentManifest(
        String shipmentId,
        String carrierCode,
        long declaredValueCents,
        double weightKg,
        boolean residentialDelivery,
        boolean liftgateRequired,
        boolean oversized) {

    public ShipmentManifest {
        Objects.requireNonNull(carrierCode, "carrierCode");
        if (shipmentId == null || shipmentId.isBlank()) {
            throw new IllegalArgumentException("shipmentId must not be blank");
        }
        if (declaredValueCents < 0L) {
            throw new IllegalArgumentException("declaredValueCents must not be negative");
        }
        if (weightKg <= 0.0) {
            throw new IllegalArgumentException("weightKg must be positive");
        }
    }
}
