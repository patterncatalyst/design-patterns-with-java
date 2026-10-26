package com.patterncatalyst.designpatterns.ch15.shipping;

/**
 * Where a shipment is headed. {@link #INTERNATIONAL} is the one value that
 * changes the Facade's orchestration: only an international shipment needs a
 * customs declaration before it can ship.
 */
public enum DestinationZone {
    LOCAL,
    REGIONAL,
    NATIONAL,
    INTERNATIONAL
}
