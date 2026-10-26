package com.patterncatalyst.designpatterns.ch28.shipping;

/** Quotes a base rate plus a per-kilogram charge. */
public final class WeightTieredRateQuoter implements RateQuoter {

    private final long baseCents;
    private final long perKgCents;

    public WeightTieredRateQuoter(long baseCents, long perKgCents) {
        this.baseCents = baseCents;
        this.perKgCents = perKgCents;
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        long cents = baseCents + Math.round(perKgCents * shipment.weightKg());
        return new RateQuote("weight-tiered", Money.ofCents(cents));
    }
}
