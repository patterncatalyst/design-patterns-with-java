package com.patterncatalyst.designpatterns.ch09.shipping;

/**
 * The varying validation step in the functional form of this chapter's
 * Template Method: a channel rule applied after the shared precondition has
 * already run.
 */
@FunctionalInterface
public interface ChannelValidator {

    void validate(Order order);
}
