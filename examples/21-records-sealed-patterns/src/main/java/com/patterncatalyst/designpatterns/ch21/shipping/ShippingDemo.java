package com.patterncatalyst.designpatterns.ch21.shipping;

/** Exercises records-as-data-carriers, sealed switches, and nested record patterns against one shipment. */
public final class ShippingDemo {

    private ShippingDemo() {
    }

    public static void main(String[] args) {
        ParcelKind refrigerated = new RefrigeratedParcel(8.0, -5, 4);
        ParcelKind oversized = new OversizedParcel(12.0, 120.0, 80.0, 60.0);

        System.out.println("== Records: validation runs once, in the compact constructor ==");
        try {
            new RefrigeratedParcel(8.0, 10, 4);
        } catch (IllegalArgumentException e) {
            System.out.println("Rejected: " + e.getMessage());
        }

        System.out.println();
        System.out.println("== ParcelKinds: billable weight, actual vs. dimensional ==");
        System.out.println("Refrigerated parcel billable weight: " + ParcelKinds.billableWeightKg(refrigerated) + "kg");
        System.out.println("Oversized parcel billable weight: " + ParcelKinds.billableWeightKg(oversized) + "kg");

        System.out.println();
        System.out.println("== ShippingEvents: describe(), including a nested record pattern ==");
        ShippingEvent quote = new QuoteRequested(refrigerated, "EAST");
        ShippingEvent exception = new DeliveryException(refrigerated, "cold storage unit failed in transit");
        ShippingEvent delivered = new Delivered(oversized);
        System.out.println(ShippingEvents.describe(quote));
        System.out.println(ShippingEvents.describe(exception));
        System.out.println(ShippingEvents.describe(delivered));

        System.out.println();
        System.out.println("== ShippingEvents: requiresPriorityHandling(), a guarded when clause ==");
        ShippingEvent coldChainBreach = new DeliveryException(new RefrigeratedParcel(8.0, -18, 0), "unit reads 9C");
        ShippingEvent routineException = new DeliveryException(new StandardParcel(3.0), "address not found");
        System.out.println("Cold-chain breach (ceiling 0C): " + ShippingEvents.requiresPriorityHandling(coldChainBreach));
        System.out.println("Routine exception on a standard parcel: " + ShippingEvents.requiresPriorityHandling(routineException));

        System.out.println();
        System.out.println("== RateRules: nesting the same rule pieces decides the quote ==");
        ParcelKind twelveKiloStandard = new StandardParcel(12.0);
        RateRule rule = new MinimumCharge(
                new ZoneSurcharge(new PerKilogram(Money.ofCents(150)), "EAST", Money.ofCents(500)),
                Money.ofCents(2_000));

        System.out.println(RateRules.describe(rule));
        System.out.println("Quote to EAST (surcharge applies, above the floor): "
                + RateRules.quote(rule, twelveKiloStandard, "EAST"));
        System.out.println("Quote to WEST (surcharge skipped, floor takes over): "
                + RateRules.quote(rule, twelveKiloStandard, "WEST"));
    }
}
