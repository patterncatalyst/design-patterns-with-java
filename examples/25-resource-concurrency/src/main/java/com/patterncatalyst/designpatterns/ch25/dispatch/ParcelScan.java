package com.patterncatalyst.designpatterns.ch25.dispatch;

import java.util.Objects;

/**
 * One scan event for a parcel moving through the dispatch hub, for example a
 * warehouse scanner reading a barcode as the parcel is staged for an
 * outbound truck.
 */
public record ParcelScan(String parcelId, String event) {

    public ParcelScan {
        Objects.requireNonNull(parcelId, "parcelId");
        Objects.requireNonNull(event, "event");
        if (parcelId.isBlank()) {
            throw new IllegalArgumentException("parcelId cannot be blank");
        }
        if (event.isBlank()) {
            throw new IllegalArgumentException("event cannot be blank");
        }
    }
}
