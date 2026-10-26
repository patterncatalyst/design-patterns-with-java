package com.patterncatalyst.designpatterns.ch33.shipping;

/**
 * The Observer contract. {@link Shipment} is the subject: it fans a legal
 * transition out to every currently subscribed listener and never has to
 * know, import, or enumerate who any of them are.
 */
@FunctionalInterface
public interface ShipmentStatusListener {

    void onStatusChanged(String shipmentId, ShipmentStatus from, ShipmentStatus to);
}
