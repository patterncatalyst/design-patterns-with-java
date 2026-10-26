package com.patterncatalyst.designpatterns.ch34.returns;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Turns the day-30 misclassification into a dollar figure, because a
 * return-eligibility bug is not an abstract correctness issue for this
 * retailer; it is a wrong amount credited back to a customer's account.
 */
class RefundImpactTest {

    private static final Money ORDER_TOTAL = Money.ofCents(20_000);

    @Test
    void draftedStrategyCreditsStoreCreditOnDay30() {
        ReturnEligibility eligibility = new DraftedReturnEligibilityStrategy().classify(30);

        assertThat(RefundPolicy.refundAmount(eligibility, ORDER_TOTAL)).isEqualTo(Money.ofCents(16_000));
    }

    @Test
    void plausibleWrongStrategyOverpaysByFortyDollarsOnDay30() {
        ReturnEligibility eligibility = new PlausibleWrongReturnEligibilityStrategy().classify(30);

        Money wrongRefund = RefundPolicy.refundAmount(eligibility, ORDER_TOTAL);

        assertThat(wrongRefund).isEqualTo(ORDER_TOTAL);
        assertThat(wrongRefund.cents() - Money.ofCents(16_000).cents()).isEqualTo(4_000L);
    }
}
