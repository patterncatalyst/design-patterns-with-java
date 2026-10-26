package com.patterncatalyst.designpatterns.ch14.shipping;

/**
 * Exercises both singleton idioms and the dependency-injection alternative
 * against the same {@code GROUND} carrier, so the identity claims and the
 * substitutability claim are observable facts at the terminal, not just
 * assertions in a test file.
 */
public final class ShippingQuoteDemo {

    private ShippingQuoteDemo() {
    }

    public static void main(String[] args) {
        System.out.println("== Enum singleton: CarrierRateTable ==");
        CarrierRateTable first = CarrierRateTable.INSTANCE;
        CarrierRateTable second = CarrierRateTable.INSTANCE;
        System.out.println("Same instance from both references: " + (first == second));
        System.out.println("GROUND base rate: " + first.baseRateFor("GROUND") + " per kg");

        System.out.println();
        System.out.println("== Holder idiom: HolderCarrierRateTable ==");
        HolderCarrierRateTable holderFirst = HolderCarrierRateTable.getInstance();
        HolderCarrierRateTable holderSecond = HolderCarrierRateTable.getInstance();
        System.out.println("Same instance from both calls: " + (holderFirst == holderSecond));
        System.out.println("GROUND base rate: " + holderFirst.baseRateFor("GROUND") + " per kg");

        System.out.println();
        System.out.println("== Hidden dependency: WarehousePricingJob reaches into the global ==");
        WarehousePricingJob warehouseJob = new WarehousePricingJob();
        System.out.println("Warehouse job GROUND quote: " + warehouseJob.quoteBaseRate("GROUND") + " per kg");

        System.out.println();
        System.out.println("== Dependency injection: ShippingQuoteService ==");
        ShippingQuoteService productionService = new ShippingQuoteService(CarrierRateTable.INSTANCE);
        System.out.println("Production service, 12kg GROUND shipment: " + productionService.quote("GROUND", 12.0));

        RateLookup promotionalRate = carrierCode -> Money.ofCents(1);
        ShippingQuoteService promotionalService = new ShippingQuoteService(promotionalRate);
        System.out.println(
                "Promotional service (fake RateLookup), 12kg shipment: " + promotionalService.quote("GROUND", 12.0));
    }
}
