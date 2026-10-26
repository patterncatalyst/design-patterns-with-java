package com.patterncatalyst.designpatterns.ch22.fulfillment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

/**
 * Runs a nightly fulfillment batch through every expression in this chapter
 * and prints what each one produces, so the agreement and the differences
 * this example's tests check for are also something you can watch happen at
 * the terminal.
 */
public final class FulfillmentDemo {

    private FulfillmentDemo() {
    }

    public static void main(String[] args) {
        ZoneRateLookup rates = new ZoneRateLookup(Map.of(
                DestinationZone.LOCAL, Money.ofCents(300),
                DestinationZone.REGIONAL, Money.ofCents(650),
                DestinationZone.NATIONAL, Money.ofCents(1200)));

        List<Shipment> batch = List.of(
                new Shipment("RETAIL-1001", 2.0, DestinationZone.REGIONAL, PackagingTier.STANDARD, 8_000),
                new Shipment("RETAIL-1002", 1.5, DestinationZone.LOCAL, PackagingTier.FRAGILE, 60_000),
                new Shipment("WHOLESALE-2001", 35.0, DestinationZone.NATIONAL, PackagingTier.OVERSIZED, 450_000),
                new Shipment("WHOLESALE-2002", 5.0, DestinationZone.NATIONAL, PackagingTier.STANDARD, 10_000),
                new Shipment("RETAIL-1003", 0.8, DestinationZone.LOCAL, PackagingTier.STANDARD, 3_000));

        System.out.println("== Enum carrying behavior: PackagingTier surcharges ==");
        for (Shipment shipment : batch) {
            System.out.println(shipment.orderId() + " (" + shipment.tier() + "): "
                    + Money.ofCents(shipment.tier().surchargeCents(shipment)) + " surcharge");
        }

        System.out.println();
        System.out.println("== Lambda satisfying the same SurchargeRule contract ==");
        SurchargeRule peakSeason = PromotionalSurcharges.holidayPeakSurcharge(25);
        SurchargeRule peakSeasonWithFragileHandling = PromotionalSurcharges.combined(
                peakSeason, PackagingTier.FRAGILE);
        System.out.println("Peak-season surcharge on RETAIL-1002 alone: "
                + Money.ofCents(peakSeason.surchargeCents(batch.get(1))));
        System.out.println("Peak-season surcharge combined with FRAGILE handling: "
                + Money.ofCents(peakSeasonWithFragileHandling.surchargeCents(batch.get(1))));

        System.out.println();
        System.out.println("== Enum Singleton: one manifest counter for the whole program ==");
        System.out.println("Manifests issued so far: " + ManifestSequence.INSTANCE.issuedCount());

        System.out.println();
        System.out.println("== Template Method as a function: validate, price, assign a manifest ==");
        for (Shipment shipment : batch) {
            boolean isWholesale = shipment.orderId().startsWith("WHOLESALE");
            FulfillmentRecord outcome = isWholesale
                    ? FulfillmentPipeline.process(shipment, FulfillmentRules::wholesaleValidate,
                            s -> FulfillmentRules.wholesalePrice(rates, s))
                    : FulfillmentPipeline.process(shipment, FulfillmentRules::retailValidate,
                            s -> FulfillmentRules.retailPrice(rates, s));
            if (outcome.accepted()) {
                System.out.println(shipment.orderId() + " -> " + outcome.manifestId() + ", total " + outcome.total());
            } else {
                System.out.println(shipment.orderId() + " -> rejected: " + outcome.rejectionReason());
            }
        }
        System.out.println("Manifests issued after the batch: " + ManifestSequence.INSTANCE.issuedCount());

        System.out.println();
        System.out.println("== Command: method references and a Runnable ==");
        NotificationLog log = new NotificationLog();
        FulfillmentNotifier notifier = new FulfillmentNotifier(List.of(log::recordEmail, log::recordSms));
        notifier.dispatchAll(batch.get(0));
        System.out.println("Notification log after dispatch: " + log.entries());

        AtomicBoolean manifestClosed = new AtomicBoolean(false);
        BatchCloseout.run(() -> {
            manifestClosed.set(true);
            System.out.println("Batch closeout ran as a plain Runnable.");
        });
        System.out.println("Manifest closed flag: " + manifestClosed.get());

        System.out.println();
        System.out.println("== Function composition: andThen vs. compose do not commute ==");
        UnaryOperator<Money> discount = PricingAdjustments.percentDiscount(0.10);
        UnaryOperator<Money> fee = PricingAdjustments.flatHandlingFee(500);
        Money startingPrice = Money.ofCents(10_000);
        System.out.println("Discount then fee (andThen):  " + discount.andThen(fee).apply(startingPrice));
        System.out.println("Fee then discount (compose):  " + discount.compose(fee).apply(startingPrice));

        System.out.println();
        System.out.println("== Streams: declarative pipelines over the batch ==");
        System.out.println("Total surcharge cents for the batch: " + FulfillmentBatch.totalSurchargeCents(batch));
        System.out.println("Shipments needing extra handling: "
                + FulfillmentBatch.needingExtraHandling(batch).stream().map(Shipment::orderId).toList());
        System.out.println("Surcharge cents by tier: " + FulfillmentBatch.surchargeByTier(batch));
        System.out.println("Shipment count by tier: " + FulfillmentBatch.countByTier(batch));

        System.out.println();
        System.out.println("== Optional: an honest answer for a zone with no base rate ==");
        Optional<Money> localRate = rates.baseRateFor(DestinationZone.LOCAL);
        Optional<Money> internationalRate = rates.baseRateFor(DestinationZone.INTERNATIONAL);
        System.out.println("LOCAL base rate: " + localRate.map(Money::toString).orElse("not priced"));
        System.out.println("INTERNATIONAL base rate: " + internationalRate.map(Money::toString).orElse("not priced"));
    }
}
