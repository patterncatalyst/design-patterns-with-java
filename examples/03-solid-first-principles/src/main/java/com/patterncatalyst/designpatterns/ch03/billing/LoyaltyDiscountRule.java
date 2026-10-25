package com.patterncatalyst.designpatterns.ch03.billing;

/**
 * A refinement of {@link PercentageDiscountRule} for repeat customers: a
 * base rate plus a bonus for every loyalty year on the account, with no
 * upper bound of its own. A long-tenured customer with a generous base rate
 * can push this rule's raw rate past 1.0; {@link
 * PercentageDiscountRule#adjustmentCents(long)} clamps it before the
 * discount is ever applied, so this subtype never has to guard against that
 * case itself.
 */
public final class LoyaltyDiscountRule extends PercentageDiscountRule {

    private final double baseRate;
    private final double bonusRatePerYear;
    private final int loyaltyYears;

    public LoyaltyDiscountRule(String name, double baseRate, double bonusRatePerYear, int loyaltyYears) {
        super(name);
        if (loyaltyYears < 0) {
            throw new IllegalArgumentException("loyaltyYears cannot be negative: " + loyaltyYears);
        }
        this.baseRate = baseRate;
        this.bonusRatePerYear = bonusRatePerYear;
        this.loyaltyYears = loyaltyYears;
    }

    @Override
    protected double discountRate(long subtotalCents) {
        return baseRate + (bonusRatePerYear * loyaltyYears);
    }
}
