package com.patterncatalyst.designpatterns.ch07.booking;

import java.util.Objects;

/**
 * Validates, prices, and records a shipment booking. This is the component
 * this chapter's Mockito suite exercises: it has exactly one collaborator
 * with a side effect worth verifying ({@link RateLedger}), and two more
 * that are plain enough to use for real in every test
 * ({@link ShipmentValidator}, {@link FreightCostCalculator}).
 */
public final class ShipmentBookingService {

    private final ShipmentValidator validator;
    private final FreightCostCalculator costCalculator;
    private final RateLedger ledger;
    private final BookingIdGenerator idGenerator;

    public ShipmentBookingService(
            ShipmentValidator validator,
            FreightCostCalculator costCalculator,
            RateLedger ledger,
            BookingIdGenerator idGenerator) {
        this.validator = Objects.requireNonNull(validator, "validator");
        this.costCalculator = Objects.requireNonNull(costCalculator, "costCalculator");
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public BookingRecord book(Shipment shipment) {
        validator.validate(shipment);
        if (ledger.capacityRemaining() <= 0) {
            throw new IllegalStateException("rate ledger is at capacity; booking rejected");
        }
        Money amount = costCalculator.cost(shipment);
        BookingRecord bookingRecord = new BookingRecord(idGenerator.nextId(), shipment, amount);
        ledger.record(bookingRecord);
        return bookingRecord;
    }
}
