package com.patterncatalyst.designpatterns.ch19.shipping.approval;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FunctionalApprovalChainTest {

    private final FunctionalApprovalChain chain = FunctionalApprovalChain.of(
            ApprovalChecks.weightLimit(40.0),
            ApprovalChecks.hazmatClearance(Set.of("US", "CA")),
            ApprovalChecks.embargo(Set.of("KP", "IR")));

    @Test
    void stopsAtTheFirstCheckThatRejects() {
        ApprovalOutcome outcome = chain.decide(new ApprovalRequest(55.0, true, "KP"));

        assertThat(outcome).isInstanceOfSatisfying(ApprovalOutcome.Rejected.class,
                rejected -> assertThat(rejected.decidedBy()).isEqualTo("weightLimit"));
    }

    @Test
    void fallsBackToManualReviewWhenEveryCheckPasses() {
        ApprovalOutcome outcome = chain.decide(new ApprovalRequest(10.0, false, "DE"));

        assertThat(outcome).isInstanceOfSatisfying(ApprovalOutcome.NeedsManualReview.class,
                review -> assertThat(review.decidedBy()).isEqualTo("functionalChain"));
    }

    @Test
    void agreesWithTheObjectChainForTheSameRequest() {
        ApprovalRequest request = new ApprovalRequest(10.0, true, "FR");

        ApprovalOutcome functionalOutcome = chain.decide(request);
        ApprovalOutcome objectOutcome = ApprovalChains.defaultChain().handle(request, new ArrayList<>());

        assertThat(functionalOutcome).isInstanceOf(ApprovalOutcome.Rejected.class);
        assertThat(objectOutcome).isInstanceOf(ApprovalOutcome.Rejected.class);
        assertThat(((ApprovalOutcome.Rejected) functionalOutcome).reason())
                .isEqualTo(((ApprovalOutcome.Rejected) objectOutcome).reason());
    }
}
