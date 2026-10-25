package com.patterncatalyst.designpatterns.ch10.shipment.state;

import com.patterncatalyst.designpatterns.ch10.shipment.ShipmentStatus;

/**
 * A closed set of shipment states. Unlike {@code ShipmentStatus}, a state
 * here can carry data specific to that status: {@link InTransit} carries a
 * carrier tracking number, {@link Cancelled} and {@link Returned} carry a
 * reason. The {@code permits} clause is the complete list; a pattern-matching
 * {@code switch} over this interface with no {@code default} fails to
 * compile the moment a new permitted type arrives without a matching case.
 */
public sealed interface ShipmentState
        permits Created, Packed, InTransit, Delivered, Cancelled, Returned, Lost {

    ShipmentStatus status();
}
