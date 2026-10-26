package com.patterncatalyst.designpatterns.ch31.shipping;

/** Flat 1-day delivery regardless of zone, base rate $30.00. */
public final class ExpressCarrierProfile implements CarrierProfile {

    @Override
    public int transitDays(String originZone, String destinationZone) {
        return 1;
    }

    @Override
    public long baseCents() {
        return 3000L;
    }
}
