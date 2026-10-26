package com.patterncatalyst.designpatterns.ch34.returns;

/**
 * The draft that passes the verification gate. It implements the return
 * policy exactly as specified: a full refund through day 29, store credit
 * from day 30 through day 59, no return accepted from day 60 on. Both lower
 * bounds are inclusive, so a customer who files a return on exactly day 30
 * has crossed into store credit territory, not stayed eligible for a full
 * refund.
 */
public final class DraftedReturnEligibilityStrategy implements ReturnEligibilityStrategy {

    @Override
    public ReturnEligibility classify(int daysSinceDelivery) {
        if (daysSinceDelivery < 30) {
            return ReturnEligibility.FULL_REFUND;
        }
        if (daysSinceDelivery < 60) {
            return ReturnEligibility.STORE_CREDIT;
        }
        return ReturnEligibility.INELIGIBLE;
    }
}
