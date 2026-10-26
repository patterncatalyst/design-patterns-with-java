package com.patterncatalyst.designpatterns.ch33.shipping;

import java.util.Objects;

/**
 * A carrier that charges a base fee plus a per-kilogram rate. Same
 * {@link RateStrategy} contract as {@link FlatRateStrategy}; the two differ
 * only in how they turn a {@link Shipment}'s weight into a {@link Money}
 * amount.
 */
public record WeightTieredRateStrategy(String carrierName, long baseCents, long perKgCents)
        implements RateStrategy {

    public WeightTieredRateStrategy {
        Objects.requireNonNull(carrierName, "carrierName");
        if (baseCents < 0 || perKgCents < 0) {
            throw new IllegalArgumentException("baseCents and perKgCents cannot be negative");
        }
    }

    @Override
    public Money quote(Shipment shipment) {
        long cents = baseCents + Math.round(perKgCents * shipment.weightKg());
        return Money.ofCents(cents);
    }
}
