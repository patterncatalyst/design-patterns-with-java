package com.patterncatalyst.designpatterns.ch16.shipping;

/**
 * The pricing math every Decorator and Proxy in this example ultimately
 * calls down into. Keeping it in one place means a rate the core, a
 * decorated stack, and a proxy all agree on comes from one formula, not
 * from three copies that happen to match today.
 */
public final class ShippingMath {

    private ShippingMath() {
    }

    public static long baseRateCents(DestinationZone zone, double weightKg) {
        long baseCents = switch (zone) {
            case LOCAL -> 300L;
            case REGIONAL -> 500L;
            case NATIONAL -> 800L;
            case INTERNATIONAL -> 1_500L;
        };
        long perKgCents = switch (zone) {
            case LOCAL -> 60L;
            case REGIONAL -> 120L;
            case NATIONAL -> 180L;
            case INTERNATIONAL -> 320L;
        };
        return baseCents + Math.round(perKgCents * weightKg);
    }

    public static long insuranceSurchargeCents(long declaredValueCents, double centsPerDeclaredDollar) {
        double declaredDollars = declaredValueCents / 100.0;
        return Math.round(declaredDollars * centsPerDeclaredDollar);
    }
}
