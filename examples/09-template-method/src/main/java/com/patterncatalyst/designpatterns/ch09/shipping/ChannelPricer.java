package com.patterncatalyst.designpatterns.ch09.shipping;

/**
 * The varying pricing step in the functional form of this chapter's
 * Template Method: how a channel turns an order's lines into a total in
 * cents.
 */
@FunctionalInterface
public interface ChannelPricer {

    long price(Order order);
}
