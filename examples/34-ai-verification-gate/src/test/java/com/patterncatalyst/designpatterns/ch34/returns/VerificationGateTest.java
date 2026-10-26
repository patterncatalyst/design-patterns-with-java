package com.patterncatalyst.designpatterns.ch34.returns;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The verification gate this chapter is about: a behavior-test suite run
 * against the exact boundary table the return policy specifies, rather than
 * against one comfortable value inside each tier. {@link
 * DraftedReturnEligibilityStrategy} passes every case in that table, which
 * is what lets it ship. {@link PlausibleWrongReturnEligibilityStrategy}
 * looks identical on a casual read and fails at exactly one boundary, which
 * is what sends it back.
 */
class VerificationGateTest {

    @ParameterizedTest(name = "day {0} classifies as {1}")
    @CsvSource({
            "0,  FULL_REFUND",
            "29, FULL_REFUND",
            "30, STORE_CREDIT",
            "59, STORE_CREDIT",
            "60, INELIGIBLE"
    })
    void draftedStrategyPassesEveryBoundaryInThePolicy(int daysSinceDelivery, ReturnEligibility expected) {
        ReturnEligibilityStrategy drafted = new DraftedReturnEligibilityStrategy();

        assertThat(drafted.classify(daysSinceDelivery))
                .as("day %d against the policy's boundary table", daysSinceDelivery)
                .isEqualTo(expected);
    }

    @Test
    void plausibleWrongStrategyFailsTheGateAtTheFirstBoundary() {
        ReturnEligibilityStrategy plausibleWrong = new PlausibleWrongReturnEligibilityStrategy();

        // Every boundary except day 30 agrees with the policy; the off-by-one
        // only shows up at the exact boundary a test that only checks a
        // comfortable value inside each tier would never reach.
        assertThat(plausibleWrong.classify(0)).isEqualTo(ReturnEligibility.FULL_REFUND);
        assertThat(plausibleWrong.classify(29)).isEqualTo(ReturnEligibility.FULL_REFUND);
        assertThat(plausibleWrong.classify(59)).isEqualTo(ReturnEligibility.STORE_CREDIT);
        assertThat(plausibleWrong.classify(60)).isEqualTo(ReturnEligibility.INELIGIBLE);

        assertThatThrownBy(() -> assertThat(plausibleWrong.classify(30))
                .as("day 30, the Full Refund / Store Credit boundary")
                .isEqualTo(ReturnEligibility.STORE_CREDIT))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("day 30");
    }
}
