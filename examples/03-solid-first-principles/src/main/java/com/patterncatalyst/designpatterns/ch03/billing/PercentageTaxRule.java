package com.patterncatalyst.designpatterns.ch03.billing;

import java.util.Objects;

/** Adds a percentage of the subtotal, such as a jurisdiction's sales tax. */
public final class PercentageTaxRule implements InvoiceRule {

    private final String name;
    private final double rate;

    public PercentageTaxRule(String name, double rate) {
        this.name = Objects.requireNonNull(name, "name");
        if (rate < 0.0) {
            throw new IllegalArgumentException("rate cannot be negative: " + rate);
        }
        this.rate = rate;
    }

    @Override
    public long adjustmentCents(long subtotalCents) {
        return Math.round(subtotalCents * rate);
    }

    @Override
    public String name() {
        return name;
    }
}
