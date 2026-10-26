package com.patterncatalyst.designpatterns.ch25.dispatch;

import java.util.Objects;

/** A request to render and print a shipping label for one parcel. */
public record ShippingLabelRequest(String parcelId, String destination) {

    public ShippingLabelRequest {
        Objects.requireNonNull(parcelId, "parcelId");
        Objects.requireNonNull(destination, "destination");
    }
}
