package com.patterncatalyst.designpatterns.ch21.shipping;

/**
 * Operations over the closed ParcelKind set, written as exhaustive
 * pattern-matching switches that deconstruct each record's components
 * directly. Neither ParcelKind nor any of its four implementing records
 * declares a method named here; adding an operation never touches them.
 */
public final class ParcelKinds {

    private static final double VOLUMETRIC_DIVISOR = 5_000.0;

    private ParcelKinds() {
    }

    /**
     * The weight a rating engine should actually charge for: an oversized
     * parcel's dimensional weight when that exceeds its actual weight,
     * otherwise the parcel's own weightKg.
     */
    public static double billableWeightKg(ParcelKind parcel) {
        return switch (parcel) {
            case OversizedParcel(var weightKg, var lengthCm, var widthCm, var heightCm) -> {
                double volumetricWeightKg = (lengthCm * widthCm * heightCm) / VOLUMETRIC_DIVISOR;
                yield Math.max(weightKg, volumetricWeightKg);
            }
            case StandardParcel(var weightKg) -> weightKg;
            case FragileParcel(var weightKg, var handlingNote) -> weightKg;
            case RefrigeratedParcel(var weightKg, var minTempC, var maxTempC) -> weightKg;
        };
    }
}
