package com.patterncatalyst.designpatterns.ch07.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryRateLedger")
class InMemoryRateLedgerTest {

    private static final int CAPACITY = 2;

    private InMemoryRateLedger ledger;

    @BeforeEach
    void setUp() {
        // Fresh ledger before every test, so no test can see another
        // test's bookings. A field assigned here, not in a constructor,
        // is what lets every test method start from the same known state
        // without hand-copying the same three lines into each one.
        ledger = new InMemoryRateLedger(CAPACITY);
    }

    @AfterEach
    void checkCapacityInvariantHeld() {
        // A post-condition check that runs after every test in this class,
        // independent of what that test itself asserted: this ledger must
        // never hold more bookings than its declared capacity, whether the
        // test above filled it, left it empty, or tried to overfill it.
        assertThat(ledger.recordedBookings()).hasSizeLessThanOrEqualTo(CAPACITY);
    }

    @Test
    @DisplayName("reports full capacity remaining when empty")
    void reportsFullCapacityWhenEmpty() {
        assertThat(ledger.capacityRemaining()).isEqualTo(CAPACITY);
    }

    @Test
    @DisplayName("decrements capacity remaining as bookings are recorded")
    void decrementsCapacityAsBookingsAreRecorded() {
        ledger.record(bookingRecord("BK-0001"));

        assertThat(ledger.capacityRemaining()).isEqualTo(1);
        assertThat(ledger.recordedBookings()).hasSize(1);
    }

    @Test
    @DisplayName("refuses a booking once capacity is exhausted")
    void refusesABookingOnceCapacityIsExhausted() {
        ledger.record(bookingRecord("BK-0001"));
        ledger.record(bookingRecord("BK-0002"));

        assertThatThrownBy(() -> ledger.record(bookingRecord("BK-0003")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("full");
    }

    private static BookingRecord bookingRecord(String bookingId) {
        Shipment shipment = new Shipment(3.0, CargoClass.STANDARD, "SEA", 10_000L);
        return new BookingRecord(bookingId, shipment, Money.ofCents(500L));
    }
}
