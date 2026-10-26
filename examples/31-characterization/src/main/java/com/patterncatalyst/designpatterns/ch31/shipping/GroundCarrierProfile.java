package com.patterncatalyst.designpatterns.ch31.shipping;

/** Same-zone ground delivery in 2 days, cross-zone in 5, base rate $5.00. */
public final class GroundCarrierProfile implements CarrierProfile {

    @Override
    public int transitDays(String originZone, String destinationZone) {
        return originZone.equals(destinationZone) ? 2 : 5;
    }

    @Override
    public long baseCents() {
        return 500L;
    }
}
