package com.patterncatalyst.designpatterns.ch30.shipping;

/**
 * The real fuel-surcharge rule: eight percent of the base cost, rounded to
 * the nearest cent. This is the production calculator
 * {@code createSurchargeCalculator()} returns by default.
 */
public final class StandardSurchargeCalculator implements SurchargeCalculator {

    private static final double SURCHARGE_RATE = 0.08;

    @Override
    public long surchargeCents(long baseCostCents) {
        return Math.round(baseCostCents * SURCHARGE_RATE);
    }
}
