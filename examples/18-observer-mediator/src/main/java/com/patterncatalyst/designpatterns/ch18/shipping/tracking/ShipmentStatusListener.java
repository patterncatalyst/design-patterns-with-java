package com.patterncatalyst.designpatterns.ch18.shipping.tracking;

/**
 * The contract a {@link ShipmentTracker} notifies through. This is a
 * functional interface on purpose: a caller with nothing but a lambda, a
 * method reference, or a named class implementing this interface are all
 * equally valid subscribers, and {@link ShipmentTracker} never has to tell
 * them apart.
 */
@FunctionalInterface
public interface ShipmentStatusListener {

    void onStatusChanged(String trackingId, ShipmentStatus oldStatus, ShipmentStatus newStatus);
}
