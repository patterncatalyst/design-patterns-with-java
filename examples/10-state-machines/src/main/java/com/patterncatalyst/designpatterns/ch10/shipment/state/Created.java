package com.patterncatalyst.designpatterns.ch10.shipment.state;

import com.patterncatalyst.designpatterns.ch10.shipment.ShipmentStatus;

public record Created() implements ShipmentState {

    @Override
    public ShipmentStatus status() {
        return ShipmentStatus.CREATED;
    }
}
