package com.patterncatalyst.designpatterns.ch24.shipping;

import java.util.Objects;

/** A quote returned by one carrier for one {@link Shipment}, in whole cents. */
public record RateQuote(String carrierId, long quotedCents) {

    public RateQuote {
        Objects.requireNonNull(carrierId, "carrierId");
        if (carrierId.isBlank()) {
            throw new IllegalArgumentException("carrierId cannot be blank");
        }
        if (quotedCents < 0) {
            throw new IllegalArgumentException("quotedCents cannot be negative: " + quotedCents);
        }
    }
}
