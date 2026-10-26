package com.patterncatalyst.designpatterns.ch22.fulfillment;

/**
 * A plain value enum with no behavior of its own. {@link ZoneRateLookup}
 * decides what each zone costs; a zone that has not been priced yet is not
 * an error in this enum, it is a missing entry {@link ZoneRateLookup}
 * reports as an {@link java.util.Optional}.
 */
public enum DestinationZone {
    LOCAL, REGIONAL, NATIONAL, INTERNATIONAL
}
