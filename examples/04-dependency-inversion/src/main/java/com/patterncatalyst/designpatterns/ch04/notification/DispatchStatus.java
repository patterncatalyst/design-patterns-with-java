package com.patterncatalyst.designpatterns.ch04.notification;

/**
 * The outcome of a single {@link MessageSender#send(OrderEvent)} call: did
 * the channel accept the message or not. This is a narrower question than
 * {@link DeliveryState}, which tracks what happens to a dispatch after it
 * was accepted.
 */
public enum DispatchStatus {
    SENT,
    FAILED
}
