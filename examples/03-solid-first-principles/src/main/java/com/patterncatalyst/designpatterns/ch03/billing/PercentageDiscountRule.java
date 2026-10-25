package com.patterncatalyst.designpatterns.ch03.billing;

import java.util.Objects;

/**
 * The base every discount rule in this example extends to stay
 * substitutable under Liskov Substitution. {@code adjustmentCents} is
 * declared {@code final} specifically so no subtype can skip the clamp that
 * protects the one invariant every caller of {@link InvoiceRule} is allowed
 * to assume of a discount: it never subtracts more than the subtotal is
 * worth. A subtype supplies only {@link #discountRate(long)}; the clamp runs
 * on whatever that hook returns, including a value past 1.0.
 */
public abstract class PercentageDiscountRule implements InvoiceRule {

    private final String name;

    protected PercentageDiscountRule(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public final long adjustmentCents(long subtotalCents) {
        double clampedRate = clampRate(discountRate(subtotalCents));
        return -Math.round(subtotalCents * clampedRate);
    }

    /**
     * The discount rate for this subtotal, before clamping. A subtype is
     * free to return a value outside {@code [0.0, 1.0]}; {@link
     * #adjustmentCents(long)} clamps the result before it is ever applied,
     * so a subtype never has to defend its own output.
     */
    protected abstract double discountRate(long subtotalCents);

    @Override
    public final String name() {
        return name;
    }

    private static double clampRate(double rate) {
        return Math.max(0.0, Math.min(1.0, rate));
    }
}
