package com.patterncatalyst.designpatterns.ch04.notification;

/**
 * What a {@link DeliveryStatusProvider} reports when asked about a dispatch
 * after the fact: still in flight, confirmed delivered, confirmed failed, or
 * a dispatch ID nobody on this channel recognizes.
 */
public enum DeliveryState {
    PENDING,
    DELIVERED,
    FAILED,
    UNKNOWN
}
