package com.patterncatalyst.designpatterns.ch07.booking;

/**
 * Hands out a booking id. This is a single-method, side-effect-free-enough
 * contract that this chapter's tests satisfy with a plain lambda rather
 * than a Mockito mock: {@code () -> "BK-0001"} is simpler to read than a
 * stub, and there is no interaction with it worth verifying.
 */
@FunctionalInterface
public interface BookingIdGenerator {
    String nextId();
}
