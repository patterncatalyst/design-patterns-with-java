package com.patterncatalyst.designpatterns.ch15.shipping;

import java.util.Objects;

/**
 * Issues a shipment's tracking number and builds the public tracking URL for
 * it. One of the three subsystems {@link ShippingFacade} orchestrates.
 */
public final class TrackingRegistry {

    private final String carrierDomain;

    public TrackingRegistry(String carrierDomain) {
        this.carrierDomain = Objects.requireNonNull(carrierDomain, "carrierDomain");
    }

    public String issueTrackingNumber(int sequence) {
        return "TRK-%04d".formatted(1000 + sequence);
    }

    public String trackingUrl(String trackingNumber) {
        return "https://track.%s/track/%s".formatted(carrierDomain, trackingNumber);
    }
}
