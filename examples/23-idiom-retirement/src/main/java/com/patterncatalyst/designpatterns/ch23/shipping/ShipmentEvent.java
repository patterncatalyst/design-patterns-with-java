package com.patterncatalyst.designpatterns.ch23.shipping;

import java.time.Duration;
import java.time.Instant;

/**
 * Every event a tracked shipment can report, closed at compile time. A
 * sealed interface with exactly these five {@code permits} replaces the
 * two older ways of modeling a closed, varying shape: a double-dispatch
 * Visitor, where adding a case means adding a method to every visitor
 * class, and an {@code instanceof}-cast chain, where a forgotten branch
 * compiles cleanly and fails at runtime on whichever shipment reaches it
 * first. Adding a sixth record here and forgetting to update
 * {@link ShipmentEventNarrator#describe(ShipmentEvent)} is a compile error
 * instead, because the switch over this type has to stay exhaustive.
 */
public sealed interface ShipmentEvent {

    record PickedUp(Instant at, String originFacility) implements ShipmentEvent {}

    record InTransit(Instant at, String currentFacility) implements ShipmentEvent {}

    record Delayed(Instant at, String reason, Duration estimatedExtraTime) implements ShipmentEvent {}

    record Delivered(Instant at, String signedBy) implements ShipmentEvent {}

    record Lost(Instant at, String lastKnownFacility) implements ShipmentEvent {}
}
