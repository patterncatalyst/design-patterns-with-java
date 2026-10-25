package com.patterncatalyst.designpatterns.ch07.booking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A real, in-process {@link RateLedger}, backed by a list with a fixed
 * capacity. This is the collaborator this chapter's test suite spies on
 * rather than mocks: most of its behavior is worth exercising for real,
 * and only {@code capacityRemaining} needs to be forced to an edge value a
 * real run would take many bookings to reach.
 */
public final class InMemoryRateLedger implements RateLedger {

    private final List<BookingRecord> bookings = new ArrayList<>();
    private final int capacity;

    public InMemoryRateLedger(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    @Override
    public void record(BookingRecord bookingRecord) {
        Objects.requireNonNull(bookingRecord, "bookingRecord");
        if (bookings.size() >= capacity) {
            throw new IllegalStateException("ledger is full");
        }
        bookings.add(bookingRecord);
    }

    @Override
    public int capacityRemaining() {
        return capacity - bookings.size();
    }

    public List<BookingRecord> recordedBookings() {
        return Collections.unmodifiableList(bookings);
    }
}
