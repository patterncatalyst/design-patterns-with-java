package com.patterncatalyst.designpatterns.ch08.shipping;

/**
 * An exact money amount, tracked as integer cents rather than a floating
 * point dollar figure. A shipping quote is a number a buyer is charged, so
 * this type exists to make rounding a one-time decision at construction
 * ({@link #ofCents(long)} or {@link #ofRoundedDollars(double)}) rather than a
 * recurring risk every time two quotes are added together.
 *
 * @param cents the amount, in integer cents; never negative
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

    public static Money ofRoundedDollars(double dollarsAndCents) {
        return new Money(Math.round(dollarsAndCents * 100.0));
    }

    public Money plus(Money other) {
        return new Money(this.cents + other.cents);
    }

    @Override
    public String toString() {
        return String.format("$%,.2f", cents / 100.0);
    }
}
