package com.patterncatalyst.designpatterns.ch33.shipping;

/**
 * An exact money amount, tracked as integer cents rather than a floating
 * point dollar figure, so a quote computed by one {@link RateStrategy} and a
 * quote restored by a {@link ShipmentCommand}'s undo can be compared for
 * equality with no epsilon anywhere in sight.
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
