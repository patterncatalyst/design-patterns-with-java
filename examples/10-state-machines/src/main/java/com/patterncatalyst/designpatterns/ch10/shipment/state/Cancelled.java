package com.patterncatalyst.designpatterns.ch10.shipment.state;

import com.patterncatalyst.designpatterns.ch10.shipment.ShipmentStatus;
import java.util.Objects;

public record Cancelled(String reason) implements ShipmentState {

    public Cancelled {
        Objects.requireNonNull(reason, "reason");
    }

    @Override
    public ShipmentStatus status() {
        return ShipmentStatus.CANCELLED;
    }
}
