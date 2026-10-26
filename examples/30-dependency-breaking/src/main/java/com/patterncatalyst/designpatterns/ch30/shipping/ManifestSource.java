package com.patterncatalyst.designpatterns.ch30.shipping;

/**
 * The minimal surface {@code summarizeManifest} actually reads: a carrier
 * code and a package count. Adapt Parameter extracts this interface so a
 * test never has to construct a heavyweight {@link CarrierManifestFile}
 * just to supply two values.
 */
public interface ManifestSource {

    String carrierCode();

    int packageCount();
}
