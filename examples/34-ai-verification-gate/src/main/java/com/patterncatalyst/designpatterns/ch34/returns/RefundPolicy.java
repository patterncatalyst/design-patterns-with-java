package com.patterncatalyst.designpatterns.ch34.returns;

/**
 * Turns a return-eligibility classification into an actual dollar amount
 * owed back to the customer. A full refund returns the order's entire
 * total; store credit returns eighty percent of it, the restocking
 * adjustment this retailer's policy applies to a late return; an
 * ineligible return returns nothing. This is where a one-comparison
 * classification bug stops being an abstract correctness question and
 * starts being a wrong number on an actual refund.
 */
public final class RefundPolicy {

    private static final double STORE_CREDIT_RATE = 0.80;

    private RefundPolicy() {
    }

    public static Money refundAmount(ReturnEligibility eligibility, Money orderTotal) {
        return switch (eligibility) {
            case FULL_REFUND -> orderTotal;
            case STORE_CREDIT -> Money.ofCents(Math.round(orderTotal.cents() * STORE_CREDIT_RATE));
            case INELIGIBLE -> Money.ofCents(0);
        };
    }
}
