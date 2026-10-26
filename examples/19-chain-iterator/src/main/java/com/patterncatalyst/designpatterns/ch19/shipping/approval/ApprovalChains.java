package com.patterncatalyst.designpatterns.ch19.shipping.approval;

import java.util.Set;

/** Wires the object-chain handlers together, innermost construction first. */
public final class ApprovalChains {

    private ApprovalChains() {}

    public static ApprovalHandler defaultChain() {
        ApprovalHandler fallback = new ManualReviewHandler();
        ApprovalHandler embargo = new EmbargoHandler(Set.of("KP", "IR"), fallback);
        ApprovalHandler hazmat = new HazmatClearanceHandler(Set.of("US", "CA"), embargo);
        return new WeightLimitHandler(40.0, hazmat);
    }
}
