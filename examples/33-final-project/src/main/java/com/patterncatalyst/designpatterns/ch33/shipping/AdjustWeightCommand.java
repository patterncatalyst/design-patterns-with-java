package com.patterncatalyst.designpatterns.ch33.shipping;

import java.util.Objects;

/**
 * The seam where Command invokes Strategy. {@code execute()} does not just
 * overwrite a number, it asks the shipment's own {@link RateStrategy} for a
 * fresh quote at the corrected weight. {@code undo()} does not call the
 * strategy a second time; it restores the exact weight and quote captured
 * before the correction ran, which is both cheaper and does not depend on
 * every future {@code RateStrategy} being a pure function of the shipment's
 * current fields.
 */
public final class AdjustWeightCommand implements ShipmentCommand {

    private final Shipment shipment;
    private final double newWeightKg;
    private double previousWeightKg;
    private Money previousQuotedRate;

    public AdjustWeightCommand(Shipment shipment, double newWeightKg) {
        this.shipment = Objects.requireNonNull(shipment, "shipment");
        this.newWeightKg = newWeightKg;
    }

    @Override
    public void execute() {
        previousWeightKg = shipment.weightKg();
        previousQuotedRate = shipment.quotedRate();
        shipment.setWeightKg(newWeightKg);
        shipment.setQuotedRate(shipment.requote());
    }

    @Override
    public void undo() {
        shipment.setWeightKg(previousWeightKg);
        shipment.setQuotedRate(previousQuotedRate);
    }
}
