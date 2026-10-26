package com.patterncatalyst.designpatterns.ch18.shipping.dispatch;

/**
 * An event a {@link DispatchColleague} raises toward its mediator. Nothing
 * here names which station reacts to which event; that routing decision
 * belongs entirely to {@link DispatchCoordinator}.
 */
public enum DispatchEvent {
    ORDER_READY_FOR_PICKUP,
    PICKED_UP_BY_CARRIER,
    OUT_FOR_DELIVERY,
    DELIVERY_EXCEPTION
}
