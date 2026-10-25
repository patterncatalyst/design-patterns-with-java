package com.patterncatalyst.designpatterns.ch10.shipment.state;

import com.patterncatalyst.designpatterns.ch10.shipment.ShipmentStatus;

public record Delivered() implements ShipmentState {

    @Override
    public ShipmentStatus status() {
        return ShipmentStatus.DELIVERED;
    }
}
