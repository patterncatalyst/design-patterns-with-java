package com.patterncatalyst.designpatterns.ch21.shipping;

import java.util.Objects;

/** base's price, raised to floor whenever base alone would quote less than that. */
public record MinimumCharge(RateRule base, Money floor) implements RateRule {

    public MinimumCharge {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(floor, "floor");
    }
}
