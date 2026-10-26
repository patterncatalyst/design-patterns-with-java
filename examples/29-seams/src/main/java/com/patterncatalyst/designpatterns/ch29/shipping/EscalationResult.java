package com.patterncatalyst.designpatterns.ch29.shipping;

import java.util.Objects;

/**
 * The outcome of checking one shipment against its promised delivery
 * window: either it is still within that window, or it has been escalated
 * to the carrier with a reason.
 */
public record EscalationResult(Shipment shipment, boolean escalated, String reason) {

    public EscalationResult {
        Objects.requireNonNull(shipment, "shipment");
        Objects.requireNonNull(reason, "reason");
    }

    public static EscalationResult escalated(Shipment shipment, String reason) {
        return new EscalationResult(shipment, true, reason);
    }

    public static EscalationResult notOverdue(Shipment shipment) {
        return new EscalationResult(shipment, false, "within the promised delivery window");
    }
}
