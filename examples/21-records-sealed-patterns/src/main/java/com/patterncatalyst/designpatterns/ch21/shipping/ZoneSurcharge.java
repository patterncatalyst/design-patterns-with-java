package com.patterncatalyst.designpatterns.ch21.shipping;

import java.util.Objects;

/** base's price, plus surcharge when the quote's destination zone matches zone exactly. */
public record ZoneSurcharge(RateRule base, String zone, Money surcharge) implements RateRule {

    public ZoneSurcharge {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(zone, "zone");
        if (zone.isBlank()) {
            throw new IllegalArgumentException("zone must not be blank");
        }
        Objects.requireNonNull(surcharge, "surcharge");
    }
}
