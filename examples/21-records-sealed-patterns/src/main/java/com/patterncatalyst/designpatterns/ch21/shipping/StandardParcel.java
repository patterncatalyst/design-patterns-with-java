package com.patterncatalyst.designpatterns.ch21.shipping;

/** A plain parcel with no special handling beyond its weight. */
public record StandardParcel(double weightKg) implements ParcelKind {

    public StandardParcel {
        if (weightKg <= 0.0) {
            throw new IllegalArgumentException("weightKg must be positive, was " + weightKg);
        }
    }
}
