package com.patterncatalyst.designpatterns.ch03.billing;

import java.util.Objects;

/**
 * A cautionary example, not a template. This rule implements {@link
 * InvoiceRule} directly instead of extending {@link PercentageDiscountRule},
 * so none of that base class's clamp ever runs. A rate past 1.0, entered by
 * hand for a one-off clearance event, discounts more than the invoice is
 * worth and pushes the total negative. {@link LoyaltyDiscountRule} shows the
 * fix: extend {@code PercentageDiscountRule} and let its {@code final}
 * {@code adjustmentCents} own the clamp instead of reimplementing the
 * percentage math from scratch.
 */
public final class ClearanceEventDiscountRule implements InvoiceRule {

    private final String name;
    private final double rate;

    public ClearanceEventDiscountRule(String name, double rate) {
        this.name = Objects.requireNonNull(name, "name");
        this.rate = rate;
    }

    @Override
    public long adjustmentCents(long subtotalCents) {
        return -Math.round(subtotalCents * rate);
    }

    @Override
    public String name() {
        return name;
    }
}
