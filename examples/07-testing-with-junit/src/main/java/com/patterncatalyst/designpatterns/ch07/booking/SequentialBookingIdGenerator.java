package com.patterncatalyst.designpatterns.ch07.booking;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The production {@link BookingIdGenerator}: a process-wide counter
 * formatted as {@code BK-0001}, {@code BK-0002}, and so on.
 */
public final class SequentialBookingIdGenerator implements BookingIdGenerator {

    private final AtomicLong counter = new AtomicLong(0);

    @Override
    public String nextId() {
        return String.format("BK-%04d", counter.incrementAndGet());
    }
}
