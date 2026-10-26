package com.patterncatalyst.designpatterns.ch31.shipping;

/**
 * Demo entry point: runs the same shipments through
 * {@link LegacyFreightQuoteCalculator} and the Fowler-refactored
 * {@link FreightQuoteCalculator} side by side, so the identical totals are
 * visible at the terminal instead of only asserted in a test, and then
 * prints the 9.99kg/10.0kg weight-tier cliff the characterization suite
 * pins.
 */
public final class Main {

    public static void main(String[] args) {
        LegacyFreightQuoteCalculator legacy = new LegacyFreightQuoteCalculator();
        FreightQuoteCalculator refactored = new FreightQuoteCalculator();

        System.out.println("== Legacy and refactored calculators agree on every shipment ==");
        printPair(legacy, refactored, "ground", "A", "A", 5.0, false, 50_000L);
        printPair(legacy, refactored, "ground", "A", "B", 10.0, false, 50_000L);
        printPair(legacy, refactored, "air", "A", "A", 20.01, true, 50_000L);
        printPair(legacy, refactored, "express", "A", "B", 2.0, false, 100_250L);

        System.out.println();
        System.out.println("== A quirk the characterization suite pins: the 10kg tier cliff ==");
        FreightQuote under = legacy.quote("ground", "A", "B", 9.99, false, 50_000L);
        FreightQuote at = legacy.quote("ground", "A", "B", 10.0, false, 50_000L);
        System.out.println("9.99kg:  surcharge " + centsToDollars(under.surchargeCents()));
        System.out.println("10.00kg: surcharge " + centsToDollars(at.surchargeCents())
                + "  (0.01kg more weight, $1.50 more surcharge)");
    }

    private static void printPair(LegacyFreightQuoteCalculator legacy, FreightQuoteCalculator refactored,
            String carrierCode, String originZone, String destinationZone, double weightKg,
            boolean isResidential, long declaredValueCents) {
        FreightQuote legacyQuote = legacy.quote(carrierCode, originZone, destinationZone, weightKg,
                isResidential, declaredValueCents);
        FreightQuote refactoredQuote = refactored.quote(new ShipmentDetails(
                carrierCode, originZone, destinationZone, weightKg, isResidential, declaredValueCents));

        String agreement = legacyQuote.equals(refactoredQuote) ? "identical" : "MISMATCH";
        System.out.printf("%-8s %s -> %s  total %s  [%s]%n", carrierCode, originZone + "/" + destinationZone,
                centsToDollars(legacyQuote.totalCents()), centsToDollars(refactoredQuote.totalCents()), agreement);
    }

    private static String centsToDollars(long cents) {
        return String.format("$%d.%02d", cents / 100, Math.abs(cents % 100));
    }
}
