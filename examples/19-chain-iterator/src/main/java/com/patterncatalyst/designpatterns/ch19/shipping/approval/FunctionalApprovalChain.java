package com.patterncatalyst.designpatterns.ch19.shipping.approval;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Chain of Responsibility as a value instead of a linked object graph: a
 * list of checks, each a {@code Function<ApprovalRequest,
 * Optional<ApprovalOutcome>>}, tried in order until one returns a decision.
 * {@link #decide} stops at the first non-empty result, the same stopping
 * rule {@link ApprovalHandler#handle} enforces for the object chain, with
 * no handler objects, no {@code next} references, and nothing to new up.
 */
public final class FunctionalApprovalChain {

    private final List<Function<ApprovalRequest, Optional<ApprovalOutcome>>> checks;

    @SafeVarargs
    private FunctionalApprovalChain(Function<ApprovalRequest, Optional<ApprovalOutcome>>... checks) {
        this.checks = List.of(checks);
    }

    @SafeVarargs
    public static FunctionalApprovalChain of(Function<ApprovalRequest, Optional<ApprovalOutcome>>... checks) {
        return new FunctionalApprovalChain(checks);
    }

    public ApprovalOutcome decide(ApprovalRequest request) {
        return checks.stream()
                .map(check -> check.apply(request))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseGet(() -> new ApprovalOutcome.NeedsManualReview("functionalChain", "no check reached a decision"));
    }
}
