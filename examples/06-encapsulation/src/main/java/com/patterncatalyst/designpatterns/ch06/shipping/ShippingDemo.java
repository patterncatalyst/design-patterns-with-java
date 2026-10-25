package com.patterncatalyst.designpatterns.ch06.shipping;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Exercises the encapsulation idioms this chapter teaches against one
 * shipment: a factory-built gateway whose concrete type is never named, a
 * cached static-factory value, and defensive copying proven at the
 * terminal rather than just asserted in a test.
 */
public final class ShippingDemo {

    private ShippingDemo() {
    }

    public static void main(String[] args) {
        System.out.println("== Public interface + package-private implementation ==");
        ShippingGateway gateway = ShippingGateway.inMemory();
        System.out.println("Concrete type the caller never names: " + gateway.getClass().getSimpleName());

        Manifest manifest = new Manifest(
                "MF-5001",
                List.of(
                        new ManifestLineItem("PALLET-STD", 4, "Standard pallet, mixed dry goods"),
                        new ManifestLineItem("CRATE-REF", 1, "Refrigerated crate, produce")),
                Instant.parse("2026-01-10T08:00:00Z"));

        Shipment shipment = gateway.createShipment("Portland, OR", "Boise, ID", manifest);
        System.out.println("Created: " + shipment.trackingId() + " (" + manifest.totalUnits() + " units)");

        System.out.println();
        System.out.println("== Static factory caching: TrackingHistory.empty() is a shared instance ==");
        System.out.println("empty() == empty(): " + (TrackingHistory.empty() == TrackingHistory.empty()));

        Shipment pickedUp = gateway.recordEvent(
                shipment.trackingId(),
                new TrackingEvent(Instant.parse("2026-01-10T09:30:00Z"), "Portland, OR", TrackingStatus.PICKED_UP));
        Shipment inTransit = gateway.recordEvent(
                shipment.trackingId(),
                new TrackingEvent(Instant.parse("2026-01-10T18:00:00Z"), "Pendleton, OR", TrackingStatus.IN_TRANSIT));

        System.out.println();
        System.out.println("== Immutability: earlier Shipment references never change ==");
        System.out.println("original history size:   " + shipment.history().events().size());
        System.out.println("picked-up history size:  " + pickedUp.history().events().size());
        System.out.println("in-transit history size: " + inTransit.history().events().size());
        System.out.println("latest event status: " + inTransit.history().latest().orElseThrow().status());

        System.out.println();
        System.out.println("== Defensive copying: mutating a caller's list does not reach inside ==");
        List<ManifestLineItem> mutableItems = new ArrayList<>();
        mutableItems.add(new ManifestLineItem("DRUM-55GAL", 2, "Sealed drum, non-hazardous"));
        Manifest defended = new Manifest("MF-5002", mutableItems, Instant.now());
        mutableItems.add(new ManifestLineItem("SNEAKY", 99, "added to the caller's list after construction"));
        System.out.println("caller's list size now: " + mutableItems.size());
        System.out.println("manifest still reports: " + defended.items().size());

        try {
            defended.items().add(new ManifestLineItem("HACK", 1, "should be rejected"));
        } catch (UnsupportedOperationException e) {
            System.out.println("manifest.items().add(...) rejected: " + e.getClass().getSimpleName());
        }
    }
}
