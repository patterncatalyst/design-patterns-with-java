package com.patterncatalyst.designpatterns.ch26.shipping;

import java.util.Objects;

/**
 * An inbound webhook delivery from a third-party carrier. {@code code} is a
 * plain string standing in for whatever a carrier's own webhook payload
 * actually sends on the wire, a value this codebase does not control and
 * cannot enumerate ahead of time.
 */
public record CarrierWebhookEvent(String code, String trackingId, String detail) {

    public CarrierWebhookEvent {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(trackingId, "trackingId");
        Objects.requireNonNull(detail, "detail");
    }
}
