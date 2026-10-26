package com.patterncatalyst.designpatterns.ch15.shipping;

/**
 * The implementation side of the Bridge: how a notification's already
 * formatted message actually reaches a recipient. Returns the delivered
 * confirmation text so a caller, and this example's tests, can observe what
 * was sent without standing up a real email or SMS provider.
 */
public interface DeliveryChannel {

    String deliver(String recipient, String message);
}
