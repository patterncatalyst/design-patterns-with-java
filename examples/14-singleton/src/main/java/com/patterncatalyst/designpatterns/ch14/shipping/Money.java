package com.patterncatalyst.designpatterns.ch14.shipping;

/**
 * An exact money amount, tracked as integer cents rather than a floating
 * point dollar figure, so a quote computed from a per-kilogram rate table
 * stays exact across every multiplication.
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

    @Override
    public String toString() {
        return String.format("$%,.2f", cents / 100.0);
    }
}
