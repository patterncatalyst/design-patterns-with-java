package com.patterncatalyst.designpatterns.ch03.billing;

/** A fixed-rate discount, such as a negotiated contract rate. */
public final class StandardDiscountRule extends PercentageDiscountRule {

    private final double rate;

    public StandardDiscountRule(String name, double rate) {
        super(name);
        this.rate = rate;
    }

    @Override
    protected double discountRate(long subtotalCents) {
        return rate;
    }
}
