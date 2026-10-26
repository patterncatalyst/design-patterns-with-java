package com.patterncatalyst.designpatterns.ch26.shipping;

import java.util.Objects;

/**
 * A shipment lifecycle event this codebase raised itself, tagged with the
 * closed {@link ShipmentEventType}.
 */
public record ShipmentEvent(ShipmentEventType type, String trackingId, String detail) {

    public ShipmentEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(trackingId, "trackingId");
        Objects.requireNonNull(detail, "detail");
    }
}
