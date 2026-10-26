package com.patterncatalyst.designpatterns.ch21.shipping;

import java.util.Objects;

/** Something went wrong moving parcel; reason is what a dispatcher reads first. */
public record DeliveryException(ParcelKind parcel, String reason) implements ShippingEvent {

    public DeliveryException {
        Objects.requireNonNull(parcel, "parcel");
        Objects.requireNonNull(reason, "reason");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }
}
