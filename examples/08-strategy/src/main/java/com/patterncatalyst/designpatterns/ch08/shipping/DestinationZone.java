package com.patterncatalyst.designpatterns.ch08.shipping;

/**
 * The declared shipping distance tier for a {@link Shipment}. A merchant's
 * checkout assigns one of these from the buyer's address before any
 * {@link RateStrategy} ever runs; the zone is an input to the pricing
 * algorithm, never something the algorithm decides for itself.
 */
public enum DestinationZone {
    LOCAL,
    REGIONAL,
    NATIONAL,
    INTERNATIONAL
}
