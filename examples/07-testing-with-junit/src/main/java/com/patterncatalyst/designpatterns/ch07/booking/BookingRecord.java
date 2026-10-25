package com.patterncatalyst.designpatterns.ch07.booking;

/**
 * A booked shipment as it is handed to {@link RateLedger#record(BookingRecord)}
 * and returned to the caller of {@link ShipmentBookingService#book(Shipment)}.
 */
public record BookingRecord(String bookingId, Shipment shipment, Money amount) {
}
