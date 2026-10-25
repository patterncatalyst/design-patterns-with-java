package com.patterncatalyst.designpatterns.ch04.notification;

/**
 * The capability of answering, after the fact, what happened to a dispatch.
 * A channel with no status API of its own, such as a one-way SMS carrier,
 * never implements this interface; there is nothing accurate it could return.
 */
public interface DeliveryStatusProvider {
    DeliveryState statusOf(String dispatchId);
}
