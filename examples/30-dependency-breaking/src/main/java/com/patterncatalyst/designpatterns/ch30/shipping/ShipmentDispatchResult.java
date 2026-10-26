package com.patterncatalyst.designpatterns.ch30.shipping;

/**
 * What {@code LegacyShipmentProcessor.dispatch} hands back: the shipment
 * identifier, the cost after the surcharge calculator ran, and whether
 * today qualifies the shipment for the express lane.
 */
public record ShipmentDispatchResult(String shipmentId, long totalCostCents, boolean expressLaneEligible) {
}
