package com.patterncatalyst.designpatterns.ch07.booking;

/**
 * Checks a {@link Shipment} against three independent business rules before
 * it is priced or booked. {@code validate} throws {@link InvalidShipmentException}
 * on the first rule it finds broken; it never returns a value, because the
 * only two outcomes this component has are "the shipment may proceed" and
 * "here is exactly why it may not."
 */
public final class ShipmentValidator {

    static final long MAX_INSURABLE_DECLARED_VALUE_CENTS = 5_000_000L;
    static final double HAZMAT_WEIGHT_CEILING_KG = 50.0;

    public void validate(Shipment shipment) {
        if (shipment.weightKg() <= 0.0) {
            throw new InvalidShipmentException("weight must be positive, got " + shipment.weightKg() + "kg");
        }
        if (shipment.declaredValueCents() <= 0L) {
            throw new InvalidShipmentException("declared value must be positive");
        }
        if (shipment.declaredValueCents() > MAX_INSURABLE_DECLARED_VALUE_CENTS) {
            throw new InvalidShipmentException("declared value exceeds the insurable limit");
        }
        if (shipment.cargoClass() == CargoClass.HAZMAT && shipment.weightKg() > HAZMAT_WEIGHT_CEILING_KG) {
            throw new InvalidShipmentException(
                    "hazmat shipments over " + HAZMAT_WEIGHT_CEILING_KG + "kg require freight approval");
        }
    }
}
