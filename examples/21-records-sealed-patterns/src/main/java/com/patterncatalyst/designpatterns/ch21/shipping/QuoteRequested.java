package com.patterncatalyst.designpatterns.ch21.shipping;

import java.util.Objects;

/** A merchant asked what it would cost to ship parcel to destinationZone. */
public record QuoteRequested(ParcelKind parcel, String destinationZone) implements ShippingEvent {

    public QuoteRequested {
        Objects.requireNonNull(parcel, "parcel");
        Objects.requireNonNull(destinationZone, "destinationZone");
        if (destinationZone.isBlank()) {
            throw new IllegalArgumentException("destinationZone must not be blank");
        }
    }
}
