package com.patterncatalyst.designpatterns.ch20.shipping;

import java.util.Objects;

/** A domestic parcel with no customs or hazard handling of its own. */
public record StandardParcel(String id, Money declaredValue, double weightKg) implements ManifestEntry {

    public StandardParcel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(declaredValue, "declaredValue");
    }
}
