package com.patterncatalyst.designpatterns.ch15.shipping;

import java.util.Objects;
import java.util.Optional;

/**
 * The one result {@link ShippingFacade} hands back: a rate quote, a printed
 * label, a tracking number and URL, and a customs declaration for the
 * shipments that need one. A caller that only wants the price still gets a
 * fully consistent manifest, never a partial one.
 *
 * @param quote the rate quote
 * @param label the rendered label text
 * @param trackingNumber the assigned tracking number
 * @param trackingUrl the public tracking URL for that number
 * @param customsDeclaration present only for an international shipment
 */
public record ShipmentManifest(
        Money quote,
        String label,
        String trackingNumber,
        String trackingUrl,
        Optional<CustomsDeclaration> customsDeclaration) {

    public ShipmentManifest {
        Objects.requireNonNull(quote, "quote");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(trackingNumber, "trackingNumber");
        Objects.requireNonNull(trackingUrl, "trackingUrl");
        Objects.requireNonNull(customsDeclaration, "customsDeclaration");
    }
}
