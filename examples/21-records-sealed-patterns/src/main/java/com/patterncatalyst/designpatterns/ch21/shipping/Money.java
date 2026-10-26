package com.patterncatalyst.designpatterns.ch21.shipping;

import java.util.Objects;

/**
 * An exact amount of money, tracked as integer cents. A rating engine that
 * used a double instead would accumulate rounding error across every
 * per-kilogram multiply and zone surcharge add; comparing two such amounts
 * for equality in a test would need an epsilon. Money never does.
 */
public record Money(long cents) {

    public Money {
        if (cents < 0) {
            throw new IllegalArgumentException("cents cannot be negative: " + cents);
        }
    }

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    public Money plus(Money other) {
        Objects.requireNonNull(other, "other");
        return new Money(this.cents + other.cents);
    }

    public Money timesWeight(double weightKg) {
        return new Money(Math.round(cents * weightKg));
    }

    public boolean isGreaterThan(Money other) {
        Objects.requireNonNull(other, "other");
        return this.cents > other.cents;
    }

    @Override
    public String toString() {
        return "$%,.2f".formatted(cents / 100.0);
    }
}
