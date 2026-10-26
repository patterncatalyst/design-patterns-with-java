package com.patterncatalyst.designpatterns.ch22.fulfillment;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * The validate and price steps {@link FulfillmentPipeline#process} needs per
 * sales channel, each a plain static method that satisfies
 * {@code Function<Shipment, Optional<String>>} or
 * {@code Function<Shipment, Money>} through a method reference at the call
 * site. Retail orders cap declared value and pay the full base rate;
 * wholesale orders require a minimum weight and receive a bulk discount on
 * the base rate before the packaging surcharge is added.
 */
public final class FulfillmentRules {

    private static final long RETAIL_DECLARED_VALUE_CAP_CENTS = 50_000L;
    private static final double RETAIL_DECLARED_VALUE_CAP_DOLLARS = RETAIL_DECLARED_VALUE_CAP_CENTS / 100.0;
    private static final double WHOLESALE_MINIMUM_WEIGHT_KG = 20.0;
    private static final double WHOLESALE_BULK_DISCOUNT = 0.15;

    private FulfillmentRules() {
    }

    public static Optional<String> retailValidate(Shipment shipment) {
        if (shipment.declaredValueCents() > RETAIL_DECLARED_VALUE_CAP_CENTS) {
            return Optional.of("retail orders cap declared value at $%.2f".formatted(RETAIL_DECLARED_VALUE_CAP_DOLLARS));
        }
        return Optional.empty();
    }

    public static Optional<String> wholesaleValidate(Shipment shipment) {
        if (shipment.weightKg() < WHOLESALE_MINIMUM_WEIGHT_KG) {
            return Optional.of("wholesale orders require at least %.0fkg".formatted(WHOLESALE_MINIMUM_WEIGHT_KG));
        }
        return Optional.empty();
    }

    public static Money retailPrice(ZoneRateLookup lookup, Shipment shipment) {
        Money base = baseRateOrThrow(lookup, shipment);
        long surchargeCents = shipment.tier().surchargeCents(shipment);
        return base.plus(Money.ofCents(surchargeCents));
    }

    public static Money wholesalePrice(ZoneRateLookup lookup, Shipment shipment) {
        Money base = baseRateOrThrow(lookup, shipment);
        Money discountedBase = PricingAdjustments.percentDiscount(WHOLESALE_BULK_DISCOUNT).apply(base);
        long surchargeCents = shipment.tier().surchargeCents(shipment);
        return discountedBase.plus(Money.ofCents(surchargeCents));
    }

    private static Money baseRateOrThrow(ZoneRateLookup lookup, Shipment shipment) {
        return lookup.baseRateFor(shipment.zone())
                .orElseThrow(() -> new NoSuchElementException("no base rate for zone " + shipment.zone()));
    }
}
