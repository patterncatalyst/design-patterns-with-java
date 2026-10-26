package com.patterncatalyst.designpatterns.ch31.shipping;

/**
 * Inherited freight-quote logic, kept exactly as received. Nobody on the
 * current team wrote this class, nobody fully trusts it, and it has no
 * tests. The carrier name is checked twice in two separate conditional
 * chains, one for transit days and one for the base rate, because whoever
 * added the "express" carrier years ago copied the first chain instead of
 * extending a shared lookup. The weight surcharge, the residential
 * surcharge, and the declared-value surcharge each carry a boundary quirk
 * that nobody documented: this class is the subject of this chapter's
 * characterization tests, not a model to imitate.
 *
 * <p>Do not "fix" the quirks here. Pinning them with a characterization
 * test, quirks included, is the point; a deliberate bug fix is a separate,
 * visible change made after the test suite exists, never a side effect of
 * reorganizing the code.
 */
public final class LegacyFreightQuoteCalculator {

    public FreightQuote quote(String carrierCode, String originZone, String destinationZone,
            double weightKg, boolean isResidential, long declaredValueCents) {
        int transitDays;
        if (carrierCode.equals("ground")) {
            if (originZone.equals(destinationZone)) {
                transitDays = 2;
            } else {
                transitDays = 5;
            }
        } else if (carrierCode.equals("air")) {
            if (originZone.equals(destinationZone)) {
                transitDays = 1;
            } else {
                transitDays = 2;
            }
        } else if (carrierCode.equals("express")) {
            transitDays = 1;
        } else {
            throw new IllegalArgumentException("unknown carrier: " + carrierCode);
        }

        long baseCents;
        if (carrierCode.equals("ground")) {
            baseCents = 500L;
        } else if (carrierCode.equals("air")) {
            baseCents = 1500L;
        } else if (carrierCode.equals("express")) {
            baseCents = 3000L;
        } else {
            throw new IllegalArgumentException("unknown carrier: " + carrierCode);
        }

        // Quirk: truncating integer division means weight is bucketed into
        // 10kg tiers, but a fractional kilogram just under a tier boundary
        // (9.99kg) pays nothing extra, while the boundary itself (10.0kg)
        // jumps a full tier.
        int weightTier = (int) (weightKg / 10.0);
        long weightSurchargeCents = weightTier * 150L;

        // Quirk: the threshold is a strict "greater than," so a shipment at
        // exactly 20.0kg does not pay the residential surcharge; one gram
        // over does.
        long residentialSurchargeCents = (isResidential && weightKg > 20.0) ? 250L : 0L;

        // Quirk: the 1% insurance surcharge on declared value over $1000 is
        // truncated with a (long) cast instead of rounded, so it is always
        // at or below the mathematically correct 1%, and a small excess
        // (under a dollar) truncates to zero even past the threshold.
        long declaredValueSurchargeCents = 0L;
        if (declaredValueCents > 100_000L) {
            double excessCents = declaredValueCents - 100_000L;
            declaredValueSurchargeCents = (long) (excessCents * 0.01);
        }

        long surchargeCents = weightSurchargeCents + residentialSurchargeCents + declaredValueSurchargeCents;
        long totalCents = baseCents + surchargeCents;
        return new FreightQuote(transitDays, baseCents, surchargeCents, totalCents);
    }
}
