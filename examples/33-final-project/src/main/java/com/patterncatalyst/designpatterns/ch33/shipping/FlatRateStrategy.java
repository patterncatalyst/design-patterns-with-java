package com.patterncatalyst.designpatterns.ch33.shipping;

import java.util.Objects;

/**
 * A carrier that charges one fixed price regardless of weight or zone. The
 * contract, a single {@link Money} quote for a {@link Shipment}, is
 * identical to {@link WeightTieredRateStrategy}'s; a caller holding a
 * {@link RateStrategy} field cannot tell which of the two it has without
 * asking.
 */
public record FlatRateStrategy(String carrierName, long flatCents) implements RateStrategy {

    public FlatRateStrategy {
        Objects.requireNonNull(carrierName, "carrierName");
        if (flatCents < 0) {
            throw new IllegalArgumentException("flatCents cannot be negative: " + flatCents);
        }
    }

    @Override
    public Money quote(Shipment shipment) {
        return Money.ofCents(flatCents);
    }
}
