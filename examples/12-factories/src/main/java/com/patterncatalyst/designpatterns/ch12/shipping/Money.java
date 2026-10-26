package com.patterncatalyst.designpatterns.ch12.shipping;

/**
 * An exact amount of money, tracked as integer cents so a quote compares
 * equal in a test assertion without an epsilon anywhere in sight.
 */
public record Money(long cents) {

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    public Money plus(Money other) {
        return new Money(this.cents + other.cents);
    }

    @Override
    public String toString() {
        return String.format("$%d.%02d", cents / 100, Math.abs(cents % 100));
    }
}
