package com.patterncatalyst.designpatterns.ch08.shipping;

/**
 * The actual pricing math, shared by every {@link RateStrategy} expression in
 * this example. Keeping the arithmetic here, called identically by the
 * named classes, the lambdas, and the {@link Carrier} enum
 * constants, is what makes it possible to assert that all three dispatch
 * mechanisms quote an identical {@link Shipment} identically: a bug fixed
 * here is fixed everywhere, rather than fixed in whichever expression
 * happened to get the patch.
 */
public final class ShippingMath {

    private ShippingMath() {
    }

    public static long weightTieredCents(long baseCents, long perKgCents, double weightKg) {
        return baseCents + Math.round(perKgCents * weightKg);
    }

    public static long distanceZoneCents(DestinationZone zone, double weightKg) {
        long perKgCents = switch (zone) {
            case LOCAL -> 150L;
            case REGIONAL -> 220L;
            case NATIONAL -> 310L;
            case INTERNATIONAL -> 540L;
        };
        return 400L + Math.round(perKgCents * weightKg);
    }

    public static long insuranceSurchargeCents(long declaredValueCents, double centsPerDeclaredDollar) {
        double declaredDollars = declaredValueCents / 100.0;
        return Math.round(declaredDollars * centsPerDeclaredDollar);
    }
}
