package com.patterncatalyst.designpatterns.ch33.shipping;

import java.util.Objects;

/**
 * The seam where Command drives State. {@code execute()} records the status
 * before applying the event, so {@code undo()} has something to restore;
 * the restore itself goes through {@link Shipment#restoreStatusForUndo},
 * not back through the transition table, because a real carrier has no
 * "un-pick-up" event to call.
 */
public final class AdvanceShipmentCommand implements ShipmentCommand {

    private final Shipment shipment;
    private final ShipmentEvent event;
    private ShipmentStatus previousStatus;

    public AdvanceShipmentCommand(Shipment shipment, ShipmentEvent event) {
        this.shipment = Objects.requireNonNull(shipment, "shipment");
        this.event = Objects.requireNonNull(event, "event");
    }

    @Override
    public void execute() {
        previousStatus = shipment.status();
        shipment.advance(event);
    }

    @Override
    public void undo() {
        shipment.restoreStatusForUndo(previousStatus);
    }
}
