package com.patterncatalyst.designpatterns.ch23.shipping;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Runs all four idiom-retirement examples from this chapter side by side:
 * the enum singleton, the record retiring a hand-written value class and
 * its defensive copying, the sealed-interface-plus-switch replacing a
 * Visitor or an {@code instanceof} chain, and the {@link java.util.Optional}
 * lookup replacing a Null Object.
 */
public final class IdiomRetirementDemo {

    private IdiomRetirementDemo() {}

    public static void main(String[] args) {
        System.out.println("== Enum singleton: ConfirmationNumberGenerator ==");
        String first = ConfirmationNumberGenerator.INSTANCE.nextConfirmationNumber("ground");
        String second = ConfirmationNumberGenerator.INSTANCE.nextConfirmationNumber("ground");
        System.out.println("First confirmation number:  " + first);
        System.out.println("Second confirmation number: " + second);

        System.out.println();
        System.out.println("== Record: ShippingAddress + DeliveryManifest ==");
        ShippingAddress address = new ShippingAddress("500 Dock Street", "Akron", "44301", "us");
        System.out.println("Address (canonicalized country code): " + address);
        DeliveryManifest manifest = new DeliveryManifest("BATCH-7", List.of(first, second));
        System.out.println("Manifest: " + manifest);

        System.out.println();
        System.out.println("== Sealed interface + switch: ShipmentEvent ==");
        List<ShipmentEvent> events = List.of(
                new ShipmentEvent.PickedUp(Instant.parse("2026-10-20T09:00:00Z"), "Akron Hub"),
                new ShipmentEvent.Delayed(
                        Instant.parse("2026-10-21T14:00:00Z"), "weather", Duration.ofHours(3)),
                new ShipmentEvent.Delivered(Instant.parse("2026-10-22T11:30:00Z"), "J. Alvarez"));
        for (ShipmentEvent event : events) {
            System.out.println(ShipmentEventNarrator.describe(event)
                    + " (notify customer: " + ShipmentEventNarrator.requiresCustomerNotification(event) + ")");
        }

        System.out.println();
        System.out.println("== Optional: PreferredCarrierDirectory + ShipmentRouter ==");
        PreferredCarrierDirectory directory =
                new PreferredCarrierDirectory(Map.of("customer-42", "freight-zone"));
        ShipmentRouter router = new ShipmentRouter(directory);
        System.out.println("customer-42 routes to:  " + router.carrierToUse("customer-42", "ground"));
        System.out.println("customer-99 routes to:  " + router.carrierToUse("customer-99", "ground"));
    }
}
