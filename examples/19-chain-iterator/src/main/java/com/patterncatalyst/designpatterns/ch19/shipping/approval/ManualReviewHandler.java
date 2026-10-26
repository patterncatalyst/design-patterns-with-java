package com.patterncatalyst.designpatterns.ch19.shipping.approval;

import java.util.Optional;

/**
 * The fallback at the end of the chain. Its {@link #attempt} never returns
 * {@link Optional#empty()}, which is what guarantees every request reaches
 * a decision: nothing in this domain is approved automatically, so the last
 * link always routes to a human reviewer rather than falling off the end of
 * the chain unhandled.
 */
public final class ManualReviewHandler extends ApprovalHandler {

    public ManualReviewHandler() {
        super("ManualReviewHandler", null);
    }

    @Override
    protected Optional<ApprovalOutcome> attempt(ApprovalRequest request) {
        return Optional.of(new ApprovalOutcome.NeedsManualReview(name(), "routed to the manual review queue"));
    }
}
