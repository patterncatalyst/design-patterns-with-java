package com.patterncatalyst.designpatterns.ch05.shipping;

/**
 * Thrown when a carrier rejects a manifest, for example a parcel over the
 * carrier's declared weight limit. A checked exception, so the exception
 * path through a {@link CarrierConnection} held inside a
 * try-with-resources block is a realistic domain failure, not a contrived
 * {@code RuntimeException} invented just to exercise cleanup.
 */
public final class ManifestRejectedException extends Exception {

    public ManifestRejectedException(String carrierEndpoint, String trackingId, double weightKg, double maxWeightKg) {
        super("carrier " + carrierEndpoint + " rejected manifest " + trackingId
                + ": weight " + weightKg + "kg exceeds the " + maxWeightKg + "kg limit");
    }
}
