package com.patterncatalyst.designpatterns.ch07.booking;

/**
 * Prices a {@link Shipment} by cargo class: a flat base rate plus a
 * per-kilogram rate that varies by class. This component takes no
 * collaborator and has no side effect, which is exactly why this chapter's
 * tests for it never reach for Mockito: a pure function is tested by
 * calling it and checking what it returns, nothing more is needed.
 */
public final class FreightCostCalculator {

    public Money cost(Shipment shipment) {
        long baseCents = baseCentsFor(shipment.cargoClass());
        long perKgCents = perKgCentsFor(shipment.cargoClass());
        long cents = baseCents + Math.round(perKgCents * shipment.weightKg());
        return Money.ofCents(cents);
    }

    private static long baseCentsFor(CargoClass cargoClass) {
        return switch (cargoClass) {
            case STANDARD -> 150L;
            case FRAGILE -> 400L;
            case HAZMAT -> 900L;
            case OVERSIZED -> 650L;
        };
    }

    private static long perKgCentsFor(CargoClass cargoClass) {
        return switch (cargoClass) {
            case STANDARD -> 120L;
            case FRAGILE -> 180L;
            case HAZMAT -> 300L;
            case OVERSIZED -> 220L;
        };
    }
}
