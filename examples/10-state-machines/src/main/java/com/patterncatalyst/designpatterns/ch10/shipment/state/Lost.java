package com.patterncatalyst.designpatterns.ch10.shipment.state;

import com.patterncatalyst.designpatterns.ch10.shipment.ShipmentStatus;

public record Lost() implements ShipmentState {

    @Override
    public ShipmentStatus status() {
        return ShipmentStatus.LOST;
    }
}
