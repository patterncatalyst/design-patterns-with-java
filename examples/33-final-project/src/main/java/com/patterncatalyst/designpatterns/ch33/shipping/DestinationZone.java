package com.patterncatalyst.designpatterns.ch33.shipping;

/**
 * A shipment's destination, graded by distance from the originating
 * warehouse. {@link WeightTieredRateStrategy} does not use this directly;
 * it exists so a carrier-specific {@link RateStrategy} can factor zone into
 * its own pricing rule without this example growing a second axis of
 * variation nobody asked for yet.
 */
public enum DestinationZone {
    LOCAL,
    REGIONAL,
    NATIONAL,
    INTERNATIONAL,
}
