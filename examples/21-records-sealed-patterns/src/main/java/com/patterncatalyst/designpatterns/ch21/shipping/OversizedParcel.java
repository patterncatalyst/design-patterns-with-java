package com.patterncatalyst.designpatterns.ch21.shipping;

/** A parcel too large for a standard belt, priced by whichever is greater: actual or volumetric weight. */
public record OversizedParcel(double weightKg, double lengthCm, double widthCm, double heightCm)
        implements ParcelKind {

    public OversizedParcel {
        if (weightKg <= 0.0) {
            throw new IllegalArgumentException("weightKg must be positive, was " + weightKg);
        }
        if (lengthCm <= 0.0 || widthCm <= 0.0 || heightCm <= 0.0) {
            throw new IllegalArgumentException(
                    "every dimension must be positive, got length=%.1f width=%.1f height=%.1f"
                            .formatted(lengthCm, widthCm, heightCm));
        }
    }
}
