package com.patterncatalyst.designpatterns.ch25.dispatch;

/** Builds a fresh {@link CarrierApiConnection}. The pool's only seam for a test double. */
@FunctionalInterface
public interface CarrierApiConnectionFactory {

    CarrierApiConnection create();
}
