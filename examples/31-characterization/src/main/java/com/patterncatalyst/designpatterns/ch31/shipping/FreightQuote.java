package com.patterncatalyst.designpatterns.ch31.shipping;

/**
 * The result both the legacy and the refactored calculator return: transit
 * days, the flat base rate, every surcharge summed, and the total. Both
 * calculators in this package build the same {@code FreightQuote}, which is
 * what lets a characterization test compare their outputs field by field
 * instead of writing two parallel result types that happen to look alike.
 */
public record FreightQuote(int transitDays, long baseCents, long surchargeCents, long totalCents) {
}
