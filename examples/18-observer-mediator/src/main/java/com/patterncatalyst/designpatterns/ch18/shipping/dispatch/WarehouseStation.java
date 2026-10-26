package com.patterncatalyst.designpatterns.ch18.shipping.dispatch;

import java.util.Objects;

/**
 * A warehouse stages an order and raises one event when it is ready to
 * move. It never references {@link CarrierStation} or
 * {@link CustomerNotificationStation}; it only knows how to {@link #raise}
 * toward whatever mediator it was bound to.
 */
public final class WarehouseStation extends DispatchColleague {

    public void markOrderReady(String orderId) {
        Objects.requireNonNull(orderId, "orderId");
        raise(DispatchEvent.ORDER_READY_FOR_PICKUP, orderId);
    }
}
