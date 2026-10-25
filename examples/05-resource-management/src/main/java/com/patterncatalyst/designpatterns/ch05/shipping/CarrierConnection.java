package com.patterncatalyst.designpatterns.ch05.shipping;

import java.util.Objects;

/**
 * A connection to a carrier's booking API. Opening one is a real resource
 * acquisition: a socket, a session token, a rate-limited connection slot on
 * the carrier's side. {@link #close()} must run exactly once no matter how
 * the code using it exits, normal return or a thrown exception, which is
 * the guarantee try-with-resources exists to make automatic.
 */
public final class CarrierConnection implements AutoCloseable {

    public static final double MAX_WEIGHT_KG = 70.0;

    private final String carrierEndpoint;
    private boolean open;

    private CarrierConnection(String carrierEndpoint) {
        this.carrierEndpoint = carrierEndpoint;
        this.open = true;
    }

    public static CarrierConnection open(String carrierEndpoint) {
        Objects.requireNonNull(carrierEndpoint, "carrierEndpoint");
        if (carrierEndpoint.isBlank()) {
            throw new IllegalArgumentException("carrierEndpoint must not be blank");
        }
        return new CarrierConnection(carrierEndpoint);
    }

    public ManifestReceipt submitManifest(Manifest manifest) throws ManifestRejectedException {
        if (!open) {
            throw new IllegalStateException("connection to " + carrierEndpoint + " is already closed");
        }
        Objects.requireNonNull(manifest, "manifest");
        if (manifest.weightKg() > MAX_WEIGHT_KG) {
            throw new ManifestRejectedException(carrierEndpoint, manifest.trackingId(), manifest.weightKg(), MAX_WEIGHT_KG);
        }
        return new ManifestReceipt(manifest.trackingId(), carrierEndpoint);
    }

    public boolean isOpen() {
        return open;
    }

    public String carrierEndpoint() {
        return carrierEndpoint;
    }

    @Override
    public void close() {
        open = false;
    }
}
