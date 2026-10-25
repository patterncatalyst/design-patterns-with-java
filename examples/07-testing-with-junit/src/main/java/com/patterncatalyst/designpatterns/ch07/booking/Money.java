package com.patterncatalyst.designpatterns.ch07.booking;

/**
 * An exact amount of money, held as integer cents so a test assertion can
 * compare two quotes for equality with no epsilon anywhere in sight.
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
