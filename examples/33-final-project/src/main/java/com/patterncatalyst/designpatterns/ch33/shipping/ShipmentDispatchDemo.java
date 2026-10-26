package com.patterncatalyst.designpatterns.ch33.shipping;

/**
 * Runs one shipment through every seam this final project composes: Builder
 * resolving a Strategy at open time, State accepting and rejecting events,
 * Command driving both State and Strategy with undo, and Observer hearing
 * about every legal transition along the way.
 */
public final class ShipmentDispatchDemo {

    public static void main(String[] args) {
        System.out.println("== 1. open(): ShipmentBuilder resolves a RateStrategy ==");
        Shipment shipment = new ShipmentBuilder()
                .id("SHIP-9100")
                .carrier("GROUND")
                .destinationZone(DestinationZone.REGIONAL)
                .weightKg(8.0)
                .build();
        System.out.println("carrier: " + shipment.carrierCode());
        System.out.println("opening quote at 8kg: " + shipment.quotedRate());

        ShipmentDispatchCenter center = new ShipmentDispatchCenter(shipment);
        try (Subscription ignored = center.subscribe(ShipmentDispatchDemo::printTransition)) {

            System.out.println();
            System.out.println("== 2-3. Observer: lifecycle events fan out as they happen ==");
            center.advance(ShipmentEvent.PICK_UP);
            center.advance(ShipmentEvent.DISPATCH);

            System.out.println();
            System.out.println("== 4. Command invokes Strategy: a warehouse rescale to 15kg ==");
            center.adjustWeight(15.0);
            System.out.println("weight now: " + shipment.weightKg() + "kg, recomputed quote: " + shipment.quotedRate());

            System.out.println();
            System.out.println("== 5. The rescale turns out to be a scanner glitch; undo it ==");
            center.undoLast();
            System.out.println("weight restored to: " + shipment.weightKg() + "kg, quote restored to: " + shipment.quotedRate());

            System.out.println();
            System.out.println("== Bonus: swapping the rate strategy mid-flight, then undoing it ==");
            RateStrategy expedited = new WeightTieredRateStrategy("ground-expedited", 900, 260);
            center.switchRateStrategy(expedited);
            System.out.println("expedited quote at 8kg: " + shipment.quotedRate());
            center.undoLast();
            System.out.println("strategy swap undone, quote restored to: " + shipment.quotedRate());

            System.out.println();
            System.out.println("== 6. Delivering the shipment ==");
            center.advance(ShipmentEvent.DELIVER);

            System.out.println();
            System.out.println("== Operator mis-scanned PICK_UP again by habit; the console rejects it ==");
            try {
                center.advance(ShipmentEvent.PICK_UP);
            } catch (IllegalShipmentTransitionException e) {
                System.out.println("rejected: " + e.getMessage());
            }
        }
    }

    private static void printTransition(String shipmentId, ShipmentStatus from, ShipmentStatus to) {
        System.out.println("  [observer] " + shipmentId + ": " + from + " -> " + to);
    }

    private ShipmentDispatchDemo() {
    }
}
