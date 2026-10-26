package com.patterncatalyst.designpatterns.ch15.shipping.globalcourier;

/**
 * GlobalCourier's own rate-lookup SDK. This class is a stand-in for
 * third-party code this book does not control and does not get to redesign:
 * weight travels in grams, a destination travels as a numeric zone code, and
 * a quote comes back as a plain {@code long}, not this app's own
 * {@code Money} type. It is {@code final} on purpose, the way a lot of real
 * vendor SDKs ship, which forecloses wrapping it by inheritance even if
 * Java's single-class inheritance made that tempting.
 */
public final class GlobalCourierRateClient {

    private final long surchargeCents;

    public GlobalCourierRateClient(long surchargeCents) {
        this.surchargeCents = surchargeCents;
    }

    /**
     * Looks up a rate in GlobalCourier's own units.
     *
     * @param weightGrams the parcel's weight in grams
     * @param zoneCode 0 = local, 1 = regional, 2 = national, 3 = international
     * @return the rate, in cents, before this app ever sees a {@code Money}
     */
    public long rateCentsForParcel(long weightGrams, int zoneCode) {
        long baseCents = weightGrams / 10 + (zoneCode * 150L);
        return baseCents + surchargeCents;
    }
}
