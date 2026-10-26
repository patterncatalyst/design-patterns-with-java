package com.patterncatalyst.designpatterns.ch34.returns;

/**
 * The shape an AI pairing partner handed back for the identical prompt: the
 * same interface, the same three outcomes, the same early-return structure
 * a reviewer skimming for "does this look right" would wave through. It
 * carries one bug, a single comparison operator on the first boundary,
 * {@code <=} instead of {@code <}, so a return filed on exactly day 30
 * stays classified as a full refund instead of crossing into store credit.
 * This class exists only so the verification-gate tests have a plausible,
 * subtly wrong draft to reject; nothing in this example ships it.
 */
public final class PlausibleWrongReturnEligibilityStrategy implements ReturnEligibilityStrategy {

    @Override
    public ReturnEligibility classify(int daysSinceDelivery) {
        if (daysSinceDelivery <= 30) { // off-by-one: should be < 30
            return ReturnEligibility.FULL_REFUND;
        }
        if (daysSinceDelivery < 60) {
            return ReturnEligibility.STORE_CREDIT;
        }
        return ReturnEligibility.INELIGIBLE;
    }
}
