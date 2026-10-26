package com.patterncatalyst.designpatterns.ch19.shipping.approval;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApprovalChainTest {

    private final ApprovalHandler chain = ApprovalChains.defaultChain();

    @Test
    void stopsAtTheFirstHandlerThatRejectsOnWeight() {
        List<String> callLog = new ArrayList<>();

        ApprovalOutcome outcome = chain.handle(new ApprovalRequest(55.0, false, "US"), callLog);

        assertThat(outcome).isInstanceOfSatisfying(ApprovalOutcome.Rejected.class,
                rejected -> assertThat(rejected.decidedBy()).isEqualTo("WeightLimitHandler"));
        assertThat(callLog).containsExactly("WeightLimitHandler");
    }

    @Test
    void passesThroughWeightBeforeRejectingOnHazmat() {
        List<String> callLog = new ArrayList<>();

        ApprovalOutcome outcome = chain.handle(new ApprovalRequest(10.0, true, "FR"), callLog);

        assertThat(outcome).isInstanceOfSatisfying(ApprovalOutcome.Rejected.class,
                rejected -> assertThat(rejected.decidedBy()).isEqualTo("HazmatClearanceHandler"));
        assertThat(callLog).containsExactly("WeightLimitHandler", "HazmatClearanceHandler");
    }

    @Test
    void passesThroughWeightAndHazmatBeforeRejectingOnEmbargo() {
        List<String> callLog = new ArrayList<>();

        ApprovalOutcome outcome = chain.handle(new ApprovalRequest(10.0, false, "KP"), callLog);

        assertThat(outcome).isInstanceOfSatisfying(ApprovalOutcome.Rejected.class,
                rejected -> assertThat(rejected.decidedBy()).isEqualTo("EmbargoHandler"));
        assertThat(callLog).containsExactly("WeightLimitHandler", "HazmatClearanceHandler", "EmbargoHandler");
    }

    @Test
    void fallsBackToManualReviewWhenNoHandlerActs() {
        List<String> callLog = new ArrayList<>();

        ApprovalOutcome outcome = chain.handle(new ApprovalRequest(10.0, false, "DE"), callLog);

        assertThat(outcome).isInstanceOfSatisfying(ApprovalOutcome.NeedsManualReview.class,
                review -> assertThat(review.decidedBy()).isEqualTo("ManualReviewHandler"));
        assertThat(callLog).containsExactly(
                "WeightLimitHandler", "HazmatClearanceHandler", "EmbargoHandler", "ManualReviewHandler");
    }
}
