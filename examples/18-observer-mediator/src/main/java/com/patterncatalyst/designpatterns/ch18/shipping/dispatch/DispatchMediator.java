package com.patterncatalyst.designpatterns.ch18.shipping.dispatch;

/**
 * The hub every {@link DispatchColleague} talks to instead of talking to
 * each other. A colleague calls {@link #notify} with itself, the event it
 * raised, and the order it concerns; the mediator decides, and only the
 * mediator decides, which other colleague, if any, responds.
 */
public interface DispatchMediator {

    void notify(DispatchColleague sender, DispatchEvent event, String orderId);
}
