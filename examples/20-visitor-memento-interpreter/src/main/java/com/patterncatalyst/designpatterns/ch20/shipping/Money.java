package com.patterncatalyst.designpatterns.ch20.shipping;

import java.util.Objects;

/**
 * An exact amount of money, tracked as integer cents. A surcharge
 * computation that used a double instead would accumulate rounding error
 * across every add() and multiply(); comparing two such amounts for
 * equality in a test would need an epsilon. Money never does.
 */
public record Money(long cents) {

    public static final Money ZERO = new Money(0);

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

    @Override
    public String toString() {
        return "$%,.2f".formatted(cents / 100.0);
    }
}
