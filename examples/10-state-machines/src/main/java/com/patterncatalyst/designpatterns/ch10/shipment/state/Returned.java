package com.patterncatalyst.designpatterns.ch10.shipment.state;

import com.patterncatalyst.designpatterns.ch10.shipment.ShipmentStatus;
import java.util.Objects;

public record Returned(String reason) implements ShipmentState {

    public Returned {
        Objects.requireNonNull(reason, "reason");
    }

    @Override
    public ShipmentStatus status() {
        return ShipmentStatus.RETURNED;
    }
}
