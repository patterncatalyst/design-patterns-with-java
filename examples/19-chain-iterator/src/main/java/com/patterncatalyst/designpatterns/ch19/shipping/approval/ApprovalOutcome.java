package com.patterncatalyst.designpatterns.ch19.shipping.approval;

/**
 * The result of running an {@link ApprovalRequest} through a chain, object
 * or functional. Nothing in this domain is approved automatically; a
 * request either fails a check outright ({@link Rejected}) or clears every
 * automatic check and is routed to a human ({@link NeedsManualReview}).
 * Sealing the interface lets a caller switch on it exhaustively with no
 * {@code default} branch to forget.
 */
public sealed interface ApprovalOutcome permits ApprovalOutcome.Rejected, ApprovalOutcome.NeedsManualReview {

    String decidedBy();

    record Rejected(String decidedBy, String reason) implements ApprovalOutcome {}

    record NeedsManualReview(String decidedBy, String reason) implements ApprovalOutcome {}
}
