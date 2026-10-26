package com.patterncatalyst.designpatterns.ch26.shipping;

/**
 * The one thing every short handler in this chapter writes to. Keeping the
 * ledger shared across both dispatch mechanisms means a routing change
 * never changes what gets recorded, only how the handler was found.
 */
public interface ShipmentLedger {

    void record(String trackingId, String note);
}
