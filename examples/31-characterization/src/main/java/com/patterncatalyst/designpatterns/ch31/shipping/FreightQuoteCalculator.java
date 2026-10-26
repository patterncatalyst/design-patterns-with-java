package com.patterncatalyst.designpatterns.ch31.shipping;

/**
 * The same freight-quote calculation as {@link LegacyFreightQuoteCalculator},
 * reshaped by three named Fowler refactorings once the characterization
 * suite made every path observable: Extract Function pulled each surcharge
 * rule into its own named method, Replace Conditional with Polymorphism
 * retired the two parallel carrier if/else chains in favor of
 * {@link CarrierProfile}, and Introduce Parameter Object collapsed six
 * positional arguments into {@link ShipmentDetails}.
 *
 * <p>Every quirk from the legacy version is preserved here: the
 * truncating weight tier, the strict residential threshold, and the
 * truncated declared-value surcharge. A refactor changes how the code is
 * organized, never what it computes; fixing one of those quirks is a
 * separate change made afterward, with its own test update.
 */
public final class FreightQuoteCalculator {

    public FreightQuote quote(ShipmentDetails details) {
        CarrierProfile profile = CarrierProfile.forCode(details.carrierCode());
        int transitDays = profile.transitDays(details.originZone(), details.destinationZone());
        long baseCents = profile.baseCents();

        long surchargeCents = weightSurchargeCents(details.weightKg())
                + residentialSurchargeCents(details.weightKg(), details.isResidential())
                + declaredValueSurchargeCents(details.declaredValueCents());

        long totalCents = baseCents + surchargeCents;
        return new FreightQuote(transitDays, baseCents, surchargeCents, totalCents);
    }

    // Extract Function: the truncating 10kg-tier rule now has a name and no
    // longer sits inline between the carrier dispatch and the next rule.
    private static long weightSurchargeCents(double weightKg) {
        int weightTier = (int) (weightKg / 10.0);
        return weightTier * 150L;
    }

    // Extract Function: the strict "greater than" threshold is unchanged,
    // only named and isolated from the weight-tier rule it sits next to.
    private static long residentialSurchargeCents(double weightKg, boolean isResidential) {
        return (isResidential && weightKg > 20.0) ? 250L : 0L;
    }

    // Extract Function: the truncating (long) cast is unchanged; this rule
    // still rounds toward zero instead of to the nearest cent.
    private static long declaredValueSurchargeCents(long declaredValueCents) {
        if (declaredValueCents <= 100_000L) {
            return 0L;
        }
        double excessCents = declaredValueCents - 100_000L;
        return (long) (excessCents * 0.01);
    }
}
