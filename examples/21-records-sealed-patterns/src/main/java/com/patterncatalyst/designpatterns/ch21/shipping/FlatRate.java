package com.patterncatalyst.designpatterns.ch21.shipping;

import java.util.Objects;

/** A fixed amount regardless of parcel weight or destination. */
public record FlatRate(Money amount) implements RateRule {

    public FlatRate {
        Objects.requireNonNull(amount, "amount");
    }
}
