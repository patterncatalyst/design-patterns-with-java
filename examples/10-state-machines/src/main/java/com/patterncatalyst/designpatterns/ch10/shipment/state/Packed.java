package com.patterncatalyst.designpatterns.ch10.shipment.state;

import com.patterncatalyst.designpatterns.ch10.shipment.ShipmentStatus;

public record Packed() implements ShipmentState {

    @Override
    public ShipmentStatus status() {
        return ShipmentStatus.PACKED;
    }
}
