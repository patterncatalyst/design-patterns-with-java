package com.patterncatalyst.designpatterns.ch30.shipping;

/**
 * What {@code LegacyShipmentProcessor} used to build with an embedded
 * {@code new StandardSurchargeCalculator()} inside {@code dispatch}.
 * Extract and Override Factory Method moves that construction behind
 * {@code createSurchargeCalculator()} so a test subclass can return a
 * different implementation without changing dispatch's logic at all.
 */
public interface SurchargeCalculator {

    long surchargeCents(long baseCostCents);
}
