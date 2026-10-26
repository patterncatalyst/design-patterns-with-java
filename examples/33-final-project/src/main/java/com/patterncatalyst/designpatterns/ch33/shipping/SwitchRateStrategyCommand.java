package com.patterncatalyst.designpatterns.ch33.shipping;

import java.util.Objects;

/**
 * Lets an operator move a shipment to a different pricing rule mid-flight,
 * for example an expedite surcharge contract negotiated after the shipment
 * was already opened, and requote against it. {@code undo()} restores both
 * the prior {@link RateStrategy} reference and the prior quote, so a
 * reversed swap leaves no trace of the strategy that was tried and
 * abandoned.
 */
public final class SwitchRateStrategyCommand implements ShipmentCommand {

    private final Shipment shipment;
    private final RateStrategy newStrategy;
    private RateStrategy previousStrategy;
    private Money previousQuotedRate;

    public SwitchRateStrategyCommand(Shipment shipment, RateStrategy newStrategy) {
        this.shipment = Objects.requireNonNull(shipment, "shipment");
        this.newStrategy = Objects.requireNonNull(newStrategy, "newStrategy");
    }

    @Override
    public void execute() {
        previousStrategy = shipment.rateStrategy();
        previousQuotedRate = shipment.quotedRate();
        shipment.setRateStrategy(newStrategy);
        shipment.setQuotedRate(shipment.requote());
    }

    @Override
    public void undo() {
        shipment.setRateStrategy(previousStrategy);
        shipment.setQuotedRate(previousQuotedRate);
    }
}
