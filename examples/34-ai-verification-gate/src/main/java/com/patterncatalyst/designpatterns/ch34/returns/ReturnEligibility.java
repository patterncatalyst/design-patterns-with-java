package com.patterncatalyst.designpatterns.ch34.returns;

/**
 * The three outcomes a return request can resolve to under this retailer's
 * policy. A classification is never partial: every return a customer files
 * lands in exactly one of these three buckets.
 */
public enum ReturnEligibility {
    FULL_REFUND,
    STORE_CREDIT,
    INELIGIBLE
}
