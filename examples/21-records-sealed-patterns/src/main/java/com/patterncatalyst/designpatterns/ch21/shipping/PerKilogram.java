package com.patterncatalyst.designpatterns.ch21.shipping;

import java.util.Objects;

/** A rate scaled by a parcel's billable weight. */
public record PerKilogram(Money ratePerKg) implements RateRule {

    public PerKilogram {
        Objects.requireNonNull(ratePerKg, "ratePerKg");
    }
}
