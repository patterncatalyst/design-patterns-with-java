package com.patterncatalyst.designpatterns.ch07.booking;

/**
 * Demo entry point: books a handful of shipments through
 * {@link ShipmentBookingService}, including one that fails validation and
 * one that finds the ledger at capacity, so the same decisions the test
 * suite proves are also visible on a terminal.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        ShipmentValidator validator = new ShipmentValidator();
        FreightCostCalculator costCalculator = new FreightCostCalculator();
        InMemoryRateLedger ledger = new InMemoryRateLedger(2);
        BookingIdGenerator idGenerator = new SequentialBookingIdGenerator();
        ShipmentBookingService service =
                new ShipmentBookingService(validator, costCalculator, ledger, idGenerator);

        System.out.println("== Booking two well-formed shipments ==");
        Shipment standard = new Shipment(3.5, CargoClass.STANDARD, "SEA", 12_000L);
        Shipment fragile = new Shipment(1.2, CargoClass.FRAGILE, "PDX", 8_000L);
        BookingRecord firstBooking = service.book(standard);
        BookingRecord secondBooking = service.book(fragile);
        System.out.println(firstBooking.bookingId() + ": " + firstBooking.amount()
                + " (" + firstBooking.shipment().cargoClass() + ", " + firstBooking.shipment().weightKg() + "kg)");
        System.out.println(secondBooking.bookingId() + ": " + secondBooking.amount()
                + " (" + secondBooking.shipment().cargoClass() + ", " + secondBooking.shipment().weightKg() + "kg)");

        System.out.println();
        System.out.println("== Rejected before pricing: a validation failure ==");
        Shipment overweightHazmat = new Shipment(75.0, CargoClass.HAZMAT, "SEA", 12_000L);
        try {
            service.book(overweightHazmat);
        } catch (InvalidShipmentException e) {
            System.out.println("Rejected: " + e.getMessage());
        }

        System.out.println();
        System.out.println("== Rejected after validation: the ledger is at capacity ==");
        Shipment thirdShipment = new Shipment(2.0, CargoClass.STANDARD, "SEA", 9_000L);
        try {
            service.book(thirdShipment);
        } catch (IllegalStateException e) {
            System.out.println("Rejected: " + e.getMessage());
        }

        System.out.println();
        System.out.println("Ledger now holds " + ledger.recordedBookings().size() + " bookings.");
    }
}
