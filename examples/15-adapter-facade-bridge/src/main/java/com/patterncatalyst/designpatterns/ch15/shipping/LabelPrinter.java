package com.patterncatalyst.designpatterns.ch15.shipping;

import java.util.Objects;

/**
 * Renders a shipping label's text. It needs a tracking number already
 * assigned, which is what makes the order {@link ShippingFacade} calls its
 * subsystems in an actual invariant rather than an arbitrary choice.
 */
public final class LabelPrinter {

    private final String carrierName;

    public LabelPrinter(String carrierName) {
        this.carrierName = Objects.requireNonNull(carrierName, "carrierName");
    }

    public String render(Shipment shipment, String trackingNumber) {
        Objects.requireNonNull(trackingNumber, "trackingNumber");
        return "%s LABEL | weight=%.1fkg | zone=%s | tracking=%s"
                .formatted(carrierName, shipment.weightKg(), shipment.zone(), trackingNumber);
    }
}
