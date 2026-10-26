package com.patterncatalyst.designpatterns.ch26.shipping;

/**
 * The short handlers both dispatch mechanisms in this chapter route to.
 * Each one is a few lines that write a single note to the ledger; none of
 * them hold state across calls or need an undo path, which is exactly what
 * makes a dispatch table the right-sized tool instead of a Strategy or
 * Command object for each one.
 */
public final class ShipmentHandlers {

    private ShipmentHandlers() {
    }

    public static void handleCreated(ShipmentEvent event, ShipmentLedger ledger) {
        ledger.record(event.trackingId(), "created: " + event.detail());
    }

    public static void handlePickedUp(ShipmentEvent event, ShipmentLedger ledger) {
        ledger.record(event.trackingId(), "picked-up: " + event.detail());
    }

    public static void handleInTransit(ShipmentEvent event, ShipmentLedger ledger) {
        ledger.record(event.trackingId(), "in-transit: " + event.detail());
    }

    public static void handleOutForDelivery(ShipmentEvent event, ShipmentLedger ledger) {
        ledger.record(event.trackingId(), "out-for-delivery: " + event.detail());
    }

    public static void handleDelivered(ShipmentEvent event, ShipmentLedger ledger) {
        ledger.record(event.trackingId(), "delivered: " + event.detail());
    }

    public static void handleException(ShipmentEvent event, ShipmentLedger ledger) {
        ledger.record(event.trackingId(), "exception: " + event.detail());
    }
}
