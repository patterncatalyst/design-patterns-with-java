package com.patterncatalyst.designpatterns.ch27.shipping;

import java.util.Objects;

/**
 * The outcome of pricing one shipment: the total cost and whether a
 * surcharge was applied to reach it, traveling together on the same value.
 * Pairing the two facts here is the direct fix for the leaky abstraction
 * {@code overengineered.ShipmentOperationsManager.wasLastCalculationSurchargeApplied()}
 * exhibits: there is no window where a caller can ask whether a surcharge
 * applied to the wrong shipment, because the answer is attached to the
 * specific result it describes rather than tracked as mutable state
 * somewhere else.
 *
 * @param totalCost the shipment's total cost after surcharge and handling fee
 * @param surchargeApplied whether a non-default surcharge multiplier
 *     contributed to {@code totalCost}
 */
public record PricingResult(Money totalCost, boolean surchargeApplied) {

    public PricingResult {
        Objects.requireNonNull(totalCost, "totalCost");
    }
}
