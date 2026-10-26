package com.patterncatalyst.designpatterns.ch22.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/**
 * {@code UnaryOperator} inherits {@code andThen} and {@code compose} from
 * {@code Function}, so composing a discount with a flat handling fee needs
 * no code of this example's own. The two adjustments do not commute: a
 * discount applied before the fee never discounts the fee itself.
 */
class PricingAdjustmentsCompositionTest {

    private static final Money STARTING_PRICE = Money.ofCents(10_000);

    @Test
    void andThenAppliesTheDiscountBeforeTheFee() {
        UnaryOperator<Money> discount = PricingAdjustments.percentDiscount(0.10);
        UnaryOperator<Money> fee = PricingAdjustments.flatHandlingFee(500);

        // 10000 * 0.90 = 9000, then + 500 = 9500.
        assertThat(discount.andThen(fee).apply(STARTING_PRICE)).isEqualTo(Money.ofCents(9_500));
    }

    @Test
    void composeAppliesTheFeeBeforeTheDiscount() {
        UnaryOperator<Money> discount = PricingAdjustments.percentDiscount(0.10);
        UnaryOperator<Money> fee = PricingAdjustments.flatHandlingFee(500);

        // 10000 + 500 = 10500, then * 0.90 = 9450.
        assertThat(discount.compose(fee).apply(STARTING_PRICE)).isEqualTo(Money.ofCents(9_450));
    }

    @Test
    void theTwoOrdersProduceDifferentResults() {
        UnaryOperator<Money> discount = PricingAdjustments.percentDiscount(0.10);
        UnaryOperator<Money> fee = PricingAdjustments.flatHandlingFee(500);

        Money viaAndThen = discount.andThen(fee).apply(STARTING_PRICE);
        Money viaCompose = discount.compose(fee).apply(STARTING_PRICE);

        assertThat(viaAndThen).isNotEqualTo(viaCompose);
    }
}
