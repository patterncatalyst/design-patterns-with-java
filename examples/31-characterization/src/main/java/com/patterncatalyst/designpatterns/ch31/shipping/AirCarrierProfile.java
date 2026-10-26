package com.patterncatalyst.designpatterns.ch31.shipping;

/** Same-zone air delivery in 1 day, cross-zone in 2, base rate $15.00. */
public final class AirCarrierProfile implements CarrierProfile {

    @Override
    public int transitDays(String originZone, String destinationZone) {
        return originZone.equals(destinationZone) ? 1 : 2;
    }

    @Override
    public long baseCents() {
        return 1500L;
    }
}
