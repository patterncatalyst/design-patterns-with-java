package com.patterncatalyst.designpatterns.ch22.fulfillment;

import java.util.function.UnaryOperator;

/**
 * Pricing adjustments as composable {@link UnaryOperator} values. A discount
 * and a flat handling fee are each a one-argument function from
 * {@link Money} to {@link Money}, and {@link UnaryOperator} inherits
 * {@code andThen} and {@code compose} from {@link java.util.function.Function}
 * for free, so chaining two adjustments into a third needs no decorator
 * class and no dedicated "apply both" method written for this example. The
 * two do not commute: a discount applied before a flat fee never discounts
 * that fee, so {@code discount.andThen(fee)} and
 * {@code discount.compose(fee)} price the same shipment differently.
 */
public final class PricingAdjustments {

    private PricingAdjustments() {
    }

    public static UnaryOperator<Money> percentDiscount(double percent) {
        return money -> money.scaledBy(1.0 - percent);
    }

    public static UnaryOperator<Money> flatHandlingFee(long feeCents) {
        return money -> money.plus(Money.ofCents(feeCents));
    }
}
