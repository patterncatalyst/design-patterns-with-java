package com.patterncatalyst.designpatterns.ch13.shipping;

import java.util.List;

/**
 * Exercises the Builder, the record wither, and the Prototype catalog end
 * to end so their behavior shows up as terminal output, not just as a
 * passing assertion.
 */
public final class ShippingDemo {

    public static void main(String[] args) {
        System.out.println("== Builder: assembling a domestic shipment ==");
        Shipment domestic = Shipment.builder()
                .trackingId("SHIP-4471")
                .carrier("Northline Freight")
                .destinationCountry("US")
                .priority(ShippingPriority.EXPEDITED)
                .build();
        System.out.println(domestic.summary());

        System.out.println();
        System.out.println("== Builder: rejecting an international shipment with no customs declaration ==");
        try {
            Shipment.builder()
                    .trackingId("SHIP-4472")
                    .carrier("Northline Freight")
                    .destinationCountry("CA")
                    .build();
        } catch (IllegalStateException rejected) {
            System.out.println("Rejected: " + rejected.getMessage());
        }

        System.out.println();
        System.out.println("== Builder: a valid international shipment with a customs declaration ==");
        CustomsDeclaration declaration = new CustomsDeclaration(
                "8517.12", "US", "Wireless router", 24_900L);
        Shipment international = Shipment.builder()
                .trackingId("SHIP-4473")
                .carrier("Northline Freight")
                .destinationCountry("CA")
                .customsDeclaration(declaration)
                .build();
        System.out.println(international.summary());

        System.out.println();
        System.out.println("== Record wither: deriving a changed CustomsDeclaration ==");
        CustomsDeclaration revalued = declaration.withDeclaredValueCents(29_900L);
        System.out.println("Original:  " + declaration);
        System.out.println("Revalued:  " + revalued);

        System.out.println();
        System.out.println("== Prototype: cloning independent packages from one catalog ==");
        PackagingCatalog catalog = new PackagingCatalog();
        catalog.register("fragile-electronics",
                new PackagingTemplate("fragile-electronics", 1.2, "FRAGILE", List.of("bubble-wrap", "foam-corner")));

        PackagingTemplate first = catalog.create("fragile-electronics");
        PackagingTemplate second = catalog.create("fragile-electronics");
        System.out.println("Before mutation: both copies report " + first.weightKg() + " kg");
        first.setWeightKg(1.6);
        first.addProtectiveMaterial("anti-static-bag");
        System.out.println("After mutating only the first copy: first=" + first.weightKg()
                + " kg, second=" + second.weightKg() + " kg");
        System.out.println("First materials:  " + first.protectiveMaterials());
        System.out.println("Second materials: " + second.protectiveMaterials());
    }

    private ShippingDemo() {
    }
}
