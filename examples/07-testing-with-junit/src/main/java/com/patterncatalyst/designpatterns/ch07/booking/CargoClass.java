package com.patterncatalyst.designpatterns.ch07.booking;

/**
 * The closed set of cargo categories a carrier accepts. Each class carries
 * its own base rate and per-kilogram rate in {@link FreightCostCalculator},
 * and {@code HAZMAT} carries an extra weight ceiling enforced by
 * {@link ShipmentValidator}.
 */
public enum CargoClass {
    STANDARD,
    FRAGILE,
    HAZMAT,
    OVERSIZED
}
