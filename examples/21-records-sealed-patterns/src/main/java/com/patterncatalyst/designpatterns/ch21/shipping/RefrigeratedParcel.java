package com.patterncatalyst.designpatterns.ch21.shipping;

/** A cold-chain parcel that must stay within a temperature band between minTempC and maxTempC. */
public record RefrigeratedParcel(double weightKg, int minTempC, int maxTempC) implements ParcelKind {

    public RefrigeratedParcel {
        if (weightKg <= 0.0) {
            throw new IllegalArgumentException("weightKg must be positive, was " + weightKg);
        }
        if (minTempC >= maxTempC) {
            throw new IllegalArgumentException(
                    "minTempC (%d) must be less than maxTempC (%d)".formatted(minTempC, maxTempC));
        }
    }
}
