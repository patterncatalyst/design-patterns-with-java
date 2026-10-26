package com.patterncatalyst.designpatterns.ch17.shipping;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Exercises the Composite tree, the Flyweight pool, and the Null Object
 * against one small shipment, printing each result so the three patterns'
 * effects show up as observable output, not just as a passing assertion.
 */
public final class ShipmentDemo {

    private ShipmentDemo() {
    }

    public static void main(String[] args) {
        PackagingSpecCatalog packagingCatalog = new PackagingSpecCatalog(Map.of(
                "STD-BOX", new PackagingSpec("STD-BOX", "corrugated cardboard", 180, HazmatClass.NONE),
                "LI-PACK", new PackagingSpec("LI-PACK", "UN 3480 lithium pack", 95, HazmatClass.LITHIUM)));
        CountryCatalog countryCatalog = new CountryCatalog(Map.of(
                "US", new Country("US", "United States", false),
                "DE", new Country("DE", "Germany", true)));

        PackagingSpec stdBox = packagingCatalog.get("STD-BOX");
        PackagingSpec liPack = packagingCatalog.get("LI-PACK");
        Country us = countryCatalog.get("US");
        Country de = countryCatalog.get("DE");

        System.out.println("== Composite: a mixed tree, one interface ==");
        ShipmentBox cartonC = new ShipmentBox("CTN-C", List.of(
                new Parcel("P-301", 2400, 5_999, stdBox, de),
                new Parcel("P-302", 2400, 5_999, stdBox, de)), 10_000);
        ShipmentBox cartonB = new ShipmentBox("CTN-B", List.of(cartonC), 10_000);
        ShipmentBox cartonA = new ShipmentBox("CTN-A", List.of(
                new Parcel("P-101", 3200, 12_499, liPack, us),
                new Parcel("P-102", 3200, 12_499, liPack, us)), 10_000);
        ShipmentBox pallet = new ShipmentBox("PAL-9", List.of(cartonA, cartonB), 20_000);

        report(cartonA);
        report(cartonC);
        report(cartonB);
        report(pallet);
        System.out.println("(Pallet's totals are Carton CTN-A's and Carton CTN-B's totals added together,");
        System.out.println(" and Carton CTN-B's total is Carton CTN-C's total, recursed through two levels.)");

        System.out.println();
        System.out.println("== Flyweight: a few descriptors, many parcels ==");
        System.out.println("Distinct packaging codes registered: 2");
        System.out.println("Flyweights materialized so far: " + packagingCatalog.materializedCount());
        for (int i = 0; i < 60; i++) {
            packagingCatalog.get(i % 2 == 0 ? "STD-BOX" : "LI-PACK");
        }
        System.out.println("After 60 more lookups across the same 2 codes, materialized: "
                + packagingCatalog.materializedCount() + " (still 2)");
        System.out.println("Two separate get(\"STD-BOX\") calls return the same instance: "
                + (packagingCatalog.get("STD-BOX") == stdBox));

        System.out.println();
        System.out.println("== Null Object: a weight-limit check with no branch for \"no handler\" ==");
        ShipmentBox unhandledCheck = new ShipmentBox("CTN-A (default, unhandled)", cartonA.contents(), 500);
        unhandledCheck.checkWeightLimit();
        System.out.println("(no output above: the default NullShipmentAlertHandler ran and did nothing)");

        ShipmentBox emptyOptionalCheck = ShipmentBox.withAlertHandler(
                "CTN-A (Optional.empty, resolved to the Null Object)", cartonA.contents(), 500, Optional.empty());
        emptyOptionalCheck.checkWeightLimit();
        System.out.println("(no output above either: Optional.empty() resolved to the same Null Object)");

        ShipmentAlertHandler loggingHandler = (boxLabel, gramsOverLimit) ->
                System.out.println("  Box " + boxLabel + " is " + gramsOverLimit + "g over its weight limit");
        ShipmentBox loggedCheck = ShipmentBox.withAlertHandler(
                "CTN-A (logged)", cartonA.contents(), 500, Optional.of(loggingHandler));
        loggedCheck.checkWeightLimit();
    }

    private static void report(ShipmentBox box) {
        System.out.printf("%s: %dg, %d units%n",
                box.label(), ShipmentNodes.totalWeightGrams(box), ShipmentNodes.itemCount(box));
    }
}
