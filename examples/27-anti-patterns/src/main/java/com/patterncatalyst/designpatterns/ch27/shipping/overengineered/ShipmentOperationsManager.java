package com.patterncatalyst.designpatterns.ch27.shipping.overengineered;

import com.patterncatalyst.designpatterns.ch27.shipping.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * OVER-ENGINEERED EXHIBIT. Resolves a rate calculator through a singleton
 * factory provider, validates that a base cost is positive, writes an audit
 * log entry for every shipment it prices, and formats a receipt string for
 * display. Line count is not the test for whether this is a god object;
 * this class is barely thirty lines. The test is the one-sentence question
 * a reviewer should be able to answer about any class: what does it do? For
 * this class the answer needs four clauses, pricing orchestration, input
 * validation, audit logging, and presentation formatting, and none of those
 * four responsibilities has anything to do with the other three.
 *
 * <p>{@link #wasLastCalculationSurchargeApplied()} is this exhibit's
 * sharpest bug: a leaky abstraction that answers a real question, did the
 * most recent calculation apply a surcharge, by tracking mutable state on
 * this manager instead of attaching the fact to the specific
 * {@link PricedShipmentReceipt} that calculation returned. A caller holding
 * an older receipt and asking this method is told whichever shipment was
 * priced most recently, a fact its name does not disclose and its signature
 * cannot express.
 */
public final class ShipmentOperationsManager {

    private final List<String> operationLog = new ArrayList<>();
    private boolean lastCalculationSurchargeApplied;

    public PricedShipmentReceipt priceShipment(String orderId, Money baseCost) {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(baseCost, "baseCost");
        if (baseCost.cents() <= 0) {
            throw new IllegalArgumentException("baseCost must be positive: " + baseCost);
        }

        RateCalculator calculator = RateCalculatorFactoryProvider.INSTANCE.factory().createCalculator();
        Money totalCost = calculator.calculate(baseCost);
        lastCalculationSurchargeApplied = ShippingSurchargeRegistry.INSTANCE.surchargeMultiplier() != 1.0;

        operationLog.add("priced " + orderId + " -> " + totalCost);
        String receipt = "Order " + orderId + ": " + totalCost;
        return new PricedShipmentReceipt(orderId, totalCost, receipt);
    }

    public List<String> operationLog() {
        return List.copyOf(operationLog);
    }

    public boolean wasLastCalculationSurchargeApplied() {
        return lastCalculationSurchargeApplied;
    }
}
