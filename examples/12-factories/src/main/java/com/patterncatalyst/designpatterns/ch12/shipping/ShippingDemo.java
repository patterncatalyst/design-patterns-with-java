package com.patterncatalyst.designpatterns.ch12.shipping;

/**
 * Exercises the Abstract Factory family and the Factory Method hierarchy
 * against real output, so the agreement the test suite checks is also
 * visible at the terminal.
 */
public final class ShippingDemo {

    public static void main(String[] args) {
        System.out.println("== Abstract Factory: two regional toolkits, one client ==");

        Shipment naShipment = new Shipment("Canada", 4.2, 18_000, Region.NORTH_AMERICA);
        RegionalShippingDesk naDesk = new RegionalShippingDesk(ShippingToolkits.forRegion(Region.NORTH_AMERICA));
        ProcessedShipment naProcessed = naDesk.process(naShipment);
        System.out.println("North America:");
        System.out.println("  label:    " + naProcessed.label());
        System.out.println("  rate:     " + naProcessed.rate());
        System.out.println("  customs:  " + naProcessed.customsDeclaration());

        Shipment euShipment = new Shipment("Germany", 4.2, 18_000, Region.EUROPEAN_UNION);
        RegionalShippingDesk euDesk = new RegionalShippingDesk(ShippingToolkits.forRegion(Region.EUROPEAN_UNION));
        ProcessedShipment euProcessed = euDesk.process(euShipment);
        System.out.println("European Union:");
        System.out.println("  label:    " + euProcessed.label());
        System.out.println("  rate:     " + euProcessed.rate());
        System.out.println("  customs:  " + euProcessed.customsDeclaration());

        System.out.println();
        System.out.println("== Abstract Factory: region chosen by name from configuration ==");
        RegionalShippingDesk lookedUpDesk = new RegionalShippingDesk(ShippingToolkits.forRegionName("north america"));
        ProcessedShipment lookedUpProcessed = lookedUpDesk.process(naShipment);
        System.out.println("Looked up (\"north america\"):");
        System.out.println("  label:    " + lookedUpProcessed.label());
        System.out.println("  rate:     " + lookedUpProcessed.rate());

        System.out.println();
        System.out.println("== Factory Method: shipment category decides the checklist ==");
        ShipmentInspector standardInspector = ShipmentInspectors.forCategory(ShipmentCategory.STANDARD);
        InspectionResult standardResult = standardInspector.inspect(naShipment);
        System.out.println("Standard inspection (" + (standardResult.passed() ? "PASSED" : "FAILED") + "):");
        standardResult.log().forEach(line -> System.out.println("  " + line));

        Shipment hazmatShipment = new Shipment("Japan", 12.0, 92_000, Region.ASIA_PACIFIC);
        ShipmentInspector hazmatInspector = ShipmentInspectors.forCategory(ShipmentCategory.HAZMAT);
        InspectionResult hazmatResult = hazmatInspector.inspect(hazmatShipment);
        System.out.println("Hazmat inspection to Japan (" + (hazmatResult.passed() ? "PASSED" : "FAILED") + "):");
        hazmatResult.log().forEach(line -> System.out.println("  " + line));

        Shipment embargoedHazmatShipment = new Shipment("North Korea", 12.0, 92_000, Region.ASIA_PACIFIC);
        InspectionResult embargoedResult = hazmatInspector.inspect(embargoedHazmatShipment);
        System.out.println("Hazmat inspection to North Korea (" + (embargoedResult.passed() ? "PASSED" : "FAILED") + "):");
        embargoedResult.log().forEach(line -> System.out.println("  " + line));

        System.out.println();
        System.out.println("== Abstract Factory: unrecognized region name rejected ==");
        try {
            ShippingToolkits.forRegionName("mars-colony");
        } catch (IllegalArgumentException e) {
            System.out.println("Rejected unknown region: " + e.getMessage());
        }
    }

    private ShippingDemo() {
    }
}
