package com.patterncatalyst.designpatterns.ch32.accessorials;

/**
 * Plays the migration out as three releases, from a cold legacy-only start
 * to every shipment on the modern calculator, the same way the test suite
 * proves it works but visible at a terminal instead of asserted in code.
 */
public final class Main {

    public static void main(String[] args) {
        AccessorialFeeCalculator legacy = new LegacyAccessorialFeeCalculator();
        AccessorialFeeCalculator modern = new ModernAccessorialFeeCalculator();
        MigratedShipmentRegistry registry = new MigratedShipmentRegistry();
        StranglerFeeCalculatorRouter router = new StranglerFeeCalculatorRouter(legacy, modern, registry);

        ShipmentManifest oceanShipment = new ShipmentManifest("SHIP-1001", "OCEAN", 100_000L, 50.0, false, false, false);
        ShipmentManifest airShipment = new ShipmentManifest("SHIP-1002", "AIR", 50_000L, 30.0, true, true, true);
        ShipmentManifest groundShipment = new ShipmentManifest("SHIP-1003", "GROUND", 20_000L, 10.0, true, false, false);

        System.out.println("== Release 1: every shipment still quoted by the legacy calculator ==");
        printQuote(router, oceanShipment);
        printQuote(router, airShipment);
        printQuote(router, groundShipment);

        System.out.println();
        System.out.println("== Release 2: SHIP-1002 migrated, the other two still legacy ==");
        registry.migrate(airShipment.shipmentId());
        printQuote(router, oceanShipment);
        printQuote(router, airShipment);
        printQuote(router, groundShipment);
        System.out.println("shipments migrated so far: " + registry.migratedCount() + " of 3");

        System.out.println();
        System.out.println("== Release 3: every shipment migrated, legacy calculator is unused ==");
        registry.migrate(oceanShipment.shipmentId());
        registry.migrate(groundShipment.shipmentId());
        printQuote(router, oceanShipment);
        printQuote(router, airShipment);
        printQuote(router, groundShipment);
        System.out.println("shipments migrated so far: " + registry.migratedCount() + " of 3");
    }

    private static void printQuote(AccessorialFeeCalculator calculator, ShipmentManifest manifest) {
        AccessorialFees fees = calculator.calculateFees(manifest);
        System.out.printf(
                "%s (%s): $%.2f%n", manifest.shipmentId(), manifest.carrierCode(), fees.totalCents() / 100.0);
    }
}
