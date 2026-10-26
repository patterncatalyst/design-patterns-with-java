package com.patterncatalyst.designpatterns.ch21.shipping;

import java.util.Objects;

/** A shipping label for parcel was bought from carrier for cost. */
public record LabelPurchased(ParcelKind parcel, String carrier, Money cost) implements ShippingEvent {

    public LabelPurchased {
        Objects.requireNonNull(parcel, "parcel");
        Objects.requireNonNull(carrier, "carrier");
        if (carrier.isBlank()) {
            throw new IllegalArgumentException("carrier must not be blank");
        }
        Objects.requireNonNull(cost, "cost");
    }
}
