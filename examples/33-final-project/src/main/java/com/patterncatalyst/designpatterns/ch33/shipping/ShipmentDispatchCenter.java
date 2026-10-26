package com.patterncatalyst.designpatterns.ch33.shipping;

import java.util.Objects;

/**
 * The composition root: the one type in this final project that knows about
 * all five patterns at once. {@link ShipmentBuilder} and {@link RateStrategy}
 * meet before a {@code ShipmentDispatchCenter} ever exists, since a caller
 * builds a {@link Shipment} first and hands it in here. From that point on,
 * every reversible operation an operator console would expose, advancing
 * the lifecycle, correcting a weight, swapping a pricing rule, goes through
 * this class's {@link #apply(ShipmentCommand)} and its {@link CommandHistory}.
 * No {@link ShipmentCommand} implementation knows about any other, and no
 * {@link ShipmentStatusListener} knows a command was involved at all.
 */
public final class ShipmentDispatchCenter {

    private final Shipment shipment;
    private final CommandHistory history = new CommandHistory();

    public ShipmentDispatchCenter(Shipment shipment) {
        this.shipment = Objects.requireNonNull(shipment, "shipment");
    }

    public Shipment shipment() {
        return shipment;
    }

    public Subscription subscribe(ShipmentStatusListener listener) {
        return shipment.subscribe(listener);
    }

    public void apply(ShipmentCommand command) {
        history.execute(command);
    }

    public void advance(ShipmentEvent event) {
        apply(new AdvanceShipmentCommand(shipment, event));
    }

    public void adjustWeight(double newWeightKg) {
        apply(new AdjustWeightCommand(shipment, newWeightKg));
    }

    public void switchRateStrategy(RateStrategy newStrategy) {
        apply(new SwitchRateStrategyCommand(shipment, newStrategy));
    }

    public boolean undoLast() {
        return history.undo();
    }

    public boolean redoLast() {
        return history.redo();
    }
}
