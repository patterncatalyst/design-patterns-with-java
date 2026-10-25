package com.patterncatalyst.designpatterns.ch03.billing;

import java.util.Objects;

/**
 * One billed line on a freight invoice: a description, a quantity, and a
 * per-unit price in integer cents. Money is never a {@code double} in this
 * domain, so a total is exact and a test assertion can compare it for
 * equality without an epsilon.
 */
public record LineItem(String description, int quantity, long unitPriceCents) {

    public LineItem {
        Objects.requireNonNull(description, "description");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description cannot be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        if (unitPriceCents < 0) {
            throw new IllegalArgumentException("unitPriceCents cannot be negative: " + unitPriceCents);
        }
    }

    /** The extended price for this line: quantity times the per-unit price. */
    public long extendedPriceCents() {
        return (long) quantity * unitPriceCents;
    }
}
