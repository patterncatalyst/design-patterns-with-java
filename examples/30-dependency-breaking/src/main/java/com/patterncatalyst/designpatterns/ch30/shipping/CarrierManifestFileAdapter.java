package com.patterncatalyst.designpatterns.ch30.shipping;

import java.util.Objects;

/**
 * Adapts a {@link CarrierManifestFile} to the narrow {@link ManifestSource}
 * contract. {@code CarrierManifestFile} itself is untouched; this adapter
 * is the only new type Adapt Parameter needs, and it is also the only
 * place that ever has to know the heavyweight type exists.
 */
public final class CarrierManifestFileAdapter implements ManifestSource {

    private final CarrierManifestFile manifestFile;

    public CarrierManifestFileAdapter(CarrierManifestFile manifestFile) {
        this.manifestFile = Objects.requireNonNull(manifestFile, "manifestFile");
    }

    @Override
    public String carrierCode() {
        return manifestFile.carrierCode();
    }

    @Override
    public int packageCount() {
        return manifestFile.packageCount();
    }
}
