package com.patterncatalyst.designpatterns.ch32.accessorials;

/**
 * The output both the legacy and the modern calculator produce: one fee
 * per accessorial charge, broken out rather than collapsed into a single
 * total, so a characterization test failure names which specific charge
 * disagreed instead of only reporting that the total was wrong.
 */
public record AccessorialFees(
        long fuelSurchargeCents,
        long residentialFeeCents,
        long liftgateFeeCents,
        long oversizeFeeCents) {

    public long totalCents() {
        return fuelSurchargeCents + residentialFeeCents + liftgateFeeCents + oversizeFeeCents;
    }
}
