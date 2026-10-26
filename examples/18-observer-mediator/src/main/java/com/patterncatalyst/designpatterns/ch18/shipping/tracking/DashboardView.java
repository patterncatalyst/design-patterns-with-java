package com.patterncatalyst.designpatterns.ch18.shipping.tracking;

/**
 * A long-lived observer a different part of the program holds onto and
 * queries later. {@code DashboardView} earns a named class, rather than a
 * one-shot lambda, because something else reads {@link #currentStatusLine()}
 * on its own schedule, after the notification that updated it has already
 * returned.
 */
public final class DashboardView implements ShipmentStatusListener {

    private volatile String currentStatusLine = "no shipment tracked yet";

    @Override
    public void onStatusChanged(String trackingId, ShipmentStatus oldStatus, ShipmentStatus newStatus) {
        currentStatusLine = trackingId + ": " + newStatus;
    }

    public String currentStatusLine() {
        return currentStatusLine;
    }
}
