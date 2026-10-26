package com.patterncatalyst.designpatterns.ch19.shipping.approval;

import java.util.List;
import java.util.Optional;

/**
 * One link in an object-based Chain of Responsibility. {@link #handle}
 * is the single method every caller ever invokes, on the first handler in
 * the chain only; it is {@code final} so every concrete handler gets the
 * same three steps automatically: record that it was consulted, ask its own
 * {@link #attempt} whether it has a decision, and otherwise delegate to the
 * next handler. A handler with no {@code next} that still declines to
 * decide reports {@link ApprovalOutcome.NeedsManualReview} rather than
 * leaving the caller to guess what an empty chain means.
 */
public abstract class ApprovalHandler {

    private final String name;
    private final ApprovalHandler next;

    protected ApprovalHandler(String name, ApprovalHandler next) {
        this.name = name;
        this.next = next;
    }

    public final ApprovalOutcome handle(ApprovalRequest request, List<String> callLog) {
        callLog.add(name);
        Optional<ApprovalOutcome> decision = attempt(request);
        if (decision.isPresent()) {
            return decision.get();
        }
        if (next != null) {
            return next.handle(request, callLog);
        }
        return new ApprovalOutcome.NeedsManualReview(name, "no handler in the chain reached a decision");
    }

    /**
     * Returns a decision when this handler's own rule fires, or
     * {@link Optional#empty()} when the request should pass to the next
     * handler. An empty result is the signal {@link #handle} reads as
     * "not my concern."
     */
    protected abstract Optional<ApprovalOutcome> attempt(ApprovalRequest request);

    protected final String name() {
        return name;
    }
}
