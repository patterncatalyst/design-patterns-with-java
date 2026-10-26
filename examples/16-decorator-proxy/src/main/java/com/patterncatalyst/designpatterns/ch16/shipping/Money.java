package com.patterncatalyst.designpatterns.ch16.shipping;

/**
 * An exact amount in cents. A quote is never a {@code double}, so decorator
 * layers can add surcharges and a test can assert the total without an
 * epsilon anywhere in sight.
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
        return new Money(this.cents + other.cents());
    }

    @Override
    public String toString() {
        return String.format("$%,.2f", cents / 100.0);
    }
}
