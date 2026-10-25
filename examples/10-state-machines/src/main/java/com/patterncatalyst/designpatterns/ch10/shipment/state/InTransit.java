package com.patterncatalyst.designpatterns.ch10.shipment.state;

import com.patterncatalyst.designpatterns.ch10.shipment.ShipmentStatus;
import java.util.Objects;

/**
 * Carries the carrier tracking number, data the enum form has nowhere to
 * put: {@code ShipmentStatus.IN_TRANSIT} is a bare constant, but this
 * {@code InTransit} is a value with a field a caller can read back.
 */
public record InTransit(String carrierTrackingNumber) implements ShipmentState {

    public InTransit {
        Objects.requireNonNull(carrierTrackingNumber, "carrierTrackingNumber");
    }

    @Override
    public ShipmentStatus status() {
        return ShipmentStatus.IN_TRANSIT;
    }
}
