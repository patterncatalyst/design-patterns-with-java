package com.patterncatalyst.designpatterns.ch21.shipping;

import java.util.Objects;

/** parcel reached its destination; the terminal event in a shipment's lifecycle. */
public record Delivered(ParcelKind parcel) implements ShippingEvent {

    public Delivered {
        Objects.requireNonNull(parcel, "parcel");
    }
}
