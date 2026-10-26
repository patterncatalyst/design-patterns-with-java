package com.patterncatalyst.designpatterns.ch15.shipping;

/**
 * An exact money amount, tracked as integer cents rather than a floating
 * point dollar figure. A rate quote, a customs declaration's value, and a
 * label's printed price are all amounts a buyer or a customs form sees
 * exactly, so this type makes rounding a one-time decision at construction
 * rather than a recurring risk every time an amount crosses a method
 * boundary.
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

    public Money plus(Money other) {
        return new Money(this.cents + other.cents);
    }

    @Override
    public String toString() {
        return String.format("$%,.2f", cents / 100.0);
    }
}
