package com.patterncatalyst.designpatterns.ch03.billing;

import java.util.Objects;

/**
 * Adds a flat fee with no relationship to the subtotal at all, such as a
 * dock-handling charge. This rule shipped after {@link InvoiceCalculator}
 * and every rule above it already existed and were already tested; adding
 * it touched exactly this one new file.
 */
public final class FlatServiceFeeRule implements InvoiceRule {

    private final String name;
    private final long feeCents;

    public FlatServiceFeeRule(String name, long feeCents) {
        this.name = Objects.requireNonNull(name, "name");
        if (feeCents < 0) {
            throw new IllegalArgumentException("feeCents cannot be negative: " + feeCents);
        }
        this.feeCents = feeCents;
    }

    @Override
    public long adjustmentCents(long subtotalCents) {
        return feeCents;
    }

    @Override
    public String name() {
        return name;
    }
}
