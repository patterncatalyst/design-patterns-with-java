package com.patterncatalyst.designpatterns.ch05.shipping;

/**
 * Confirmation that a carrier accepted a {@link Manifest}.
 */
public record ManifestReceipt(String trackingId, String carrierEndpoint) {
}
