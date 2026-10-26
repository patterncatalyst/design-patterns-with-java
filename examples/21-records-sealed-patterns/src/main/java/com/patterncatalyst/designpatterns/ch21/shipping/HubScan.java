package com.patterncatalyst.designpatterns.ch21.shipping;

import java.util.Objects;

/** parcel was scanned in at a sorting hub identified by hubCode. */
public record HubScan(ParcelKind parcel, String hubCode) implements ShippingEvent {

    public HubScan {
        Objects.requireNonNull(parcel, "parcel");
        Objects.requireNonNull(hubCode, "hubCode");
        if (hubCode.isBlank()) {
            throw new IllegalArgumentException("hubCode must not be blank");
        }
    }
}
