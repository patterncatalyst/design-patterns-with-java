package com.patterncatalyst.designpatterns.ch09.shipping;

/**
 * The varying recording step in the functional form of this chapter's
 * Template Method: how a channel writes its ledger entry once a total is
 * known.
 */
@FunctionalInterface
public interface ChannelRecorder {

    String record(Order order, long totalCents);
}
