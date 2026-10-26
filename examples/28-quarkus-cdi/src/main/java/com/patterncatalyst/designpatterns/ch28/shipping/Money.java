package com.patterncatalyst.designpatterns.ch28.shipping;

import java.util.Objects;

/**
 * An exact integer-cents amount, so a quote never needs an epsilon in an
 * assertion.
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
        return new Money(cents + other.cents);
    }

    @Override
    public String toString() {
        return String.format("$%d.%02d", cents / 100, cents % 100);
    }
}
