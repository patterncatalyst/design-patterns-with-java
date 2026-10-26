package com.patterncatalyst.designpatterns.ch20.shipping;

import java.util.Objects;

/** A parcel routed through a freight desk instead of a standard belt, by its own dimensions. */
public record OversizedParcel(
        String id, Money declaredValue, double weightKg, double lengthCm, double widthCm, double heightCm)
        implements ManifestEntry {

    public OversizedParcel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(declaredValue, "declaredValue");
    }
}
