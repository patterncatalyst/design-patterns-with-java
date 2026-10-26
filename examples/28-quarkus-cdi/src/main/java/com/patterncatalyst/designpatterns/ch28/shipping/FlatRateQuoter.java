package com.patterncatalyst.designpatterns.ch28.shipping;

/** Quotes every shipment at the same flat rate regardless of weight. */
public final class FlatRateQuoter implements RateQuoter {

    private final long flatCents;

    public FlatRateQuoter(long flatCents) {
        this.flatCents = flatCents;
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        return new RateQuote("flat-rate", Money.ofCents(flatCents));
    }
}
