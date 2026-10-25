package com.patterncatalyst.designpatterns.ch07.booking;

/**
 * Durably records a booking and reports how much room is left before the
 * ledger must stop accepting new bookings. A real implementation writes to
 * a database or a message log, which is exactly the side effect that makes
 * this the one collaborator in this chapter's domain worth mocking: a test
 * cares that {@code record} was called correctly, not that a database row
 * exists.
 */
public interface RateLedger {
    void record(BookingRecord bookingRecord);

    int capacityRemaining();
}
