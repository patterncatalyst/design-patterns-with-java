package com.patterncatalyst.designpatterns.ch22.fulfillment;

/**
 * An exact integer amount in cents. Every price in this example is a
 * {@code Money}, never a {@code double}, so a test assertion can compare two
 * amounts for equality without an epsilon anywhere in sight.
 */
public record Money(long cents) {

    public static final Money ZERO = new Money(0L);

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    public Money plus(Money other) {
        return new Money(cents + other.cents());
    }

    /**
     * Scales this amount by a factor, rounding to the nearest cent. A
     * discount applies a factor below {@code 1.0}; a tax applies a factor
     * above it.
     */
    public Money scaledBy(double factor) {
        return new Money(Math.round(cents * factor));
    }

    @Override
    public String toString() {
        long wholeDollars = cents / 100;
        long remainderCents = Math.abs(cents % 100);
        return "$%d.%02d".formatted(wholeDollars, remainderCents);
    }
}
