package com.patterncatalyst.designpatterns.ch03.billing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class LiskovSubstitutionTest {

    private static final long SUBTOTAL_CENTS = 192_000;

    /**
     * Written once against {@link InvoiceRule}, the base type, never against
     * a concrete discount class. Every {@code PercentageDiscountRule}
     * subtype has to behave correctly when called through this function for
     * substitution to hold.
     */
    private static long adjustmentThroughBaseType(InvoiceRule rule, long subtotalCents) {
        return rule.adjustmentCents(subtotalCents);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.05, 0.5, 1.0, 1.5, 5.0, -3.0})
    void standardDiscountNeverExceedsTheSubtotalRegardlessOfConfiguredRate(double rate) {
        StandardDiscountRule discount = new StandardDiscountRule("contract discount", rate);

        long adjustment = adjustmentThroughBaseType(discount, SUBTOTAL_CENTS);

        assertThat(adjustment).isBetween(-SUBTOTAL_CENTS, 0L);
    }

    @Test
    void loyaltyDiscountInheritsTheClampEvenWhenItsCombinedRateExceedsOneHundredPercent() {
        // A 95% negotiated base rate plus 50 loyalty years would, unclamped,
        // discount at a 145% rate: more than the invoice is worth.
        // LoyaltyDiscountRule never defends against that itself; the clamp
        // it inherits from PercentageDiscountRule does.
        LoyaltyDiscountRule loyalty = new LoyaltyDiscountRule("loyalty discount", 0.95, 0.01, 50);

        long adjustment = adjustmentThroughBaseType(loyalty, SUBTOTAL_CENTS);

        assertThat(adjustment).isEqualTo(-SUBTOTAL_CENTS);
    }

    @Test
    void standardAndLoyaltyDiscountsAreBothSubstitutableForTheBaseType() {
        InvoiceRule standard = new StandardDiscountRule("contract discount", 0.05);
        InvoiceRule loyalty = new LoyaltyDiscountRule("loyalty discount", 0.05, 0.01, 6);

        // Same function, same subtotal, two different concrete subtypes.
        // Substitutability means neither call site needs to know which one
        // it holds.
        assertThat(adjustmentThroughBaseType(standard, SUBTOTAL_CENTS)).isEqualTo(-9_600L);
        assertThat(adjustmentThroughBaseType(loyalty, SUBTOTAL_CENTS)).isEqualTo(-21_120L);
    }

    @Test
    void clearanceEventRuleViolatesTheInvariantEveryDiscountRuleIsExpectedToHonor() {
        // ClearanceEventDiscountRule implements InvoiceRule directly instead
        // of extending PercentageDiscountRule, so the shared clamp never
        // runs. A 150% rate discounts more than the subtotal is worth,
        // something no PercentageDiscountRule subtype can do. This is the
        // counter-example: a type that satisfies InvoiceRule's method
        // signature but not the contract every caller is entitled to assume.
        InvoiceRule clearance = new ClearanceEventDiscountRule("clearance event", 1.5);

        long adjustment = adjustmentThroughBaseType(clearance, SUBTOTAL_CENTS);

        assertThat(adjustment).isLessThan(-SUBTOTAL_CENTS);
    }

    @Test
    void anInvoiceCalculatorCannotTellTheDifferenceBetweenSubtotalAndLoyaltyDiscounts() {
        Invoice invoice = new Invoice("INV-3087", "Anchor Point Imports",
                java.util.List.of(
                        new LineItem("Freight handling", 2, 82_500),
                        new LineItem("Warehouse storage", 6, 4_500)));

        InvoiceCalculator withStandardDiscount = new InvoiceCalculator(java.util.List.of(
                new StandardDiscountRule("contract discount", 0.05)));
        InvoiceCalculator withLoyaltyDiscount = new InvoiceCalculator(java.util.List.of(
                new LoyaltyDiscountRule("loyalty discount", 0.05, 0.0, 0)));

        // A loyalty customer with zero loyalty years and the same base rate
        // as the standard discount must total identically; the calculator
        // never has to know, or care, which concrete rule it was handed.
        assertThat(withStandardDiscount.totalsFor(invoice).totalCents())
                .isEqualTo(withLoyaltyDiscount.totalsFor(invoice).totalCents());
    }
}
