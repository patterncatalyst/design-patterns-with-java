package com.patterncatalyst.designpatterns.ch20.shipping;

import java.util.Objects;

/** A parcel requiring a hazard placard and a UN number, handled domestically. */
public record HazardousParcel(
        String id, Money declaredValue, double weightKg, String hazardClass, String unNumber)
        implements ManifestEntry {

    public HazardousParcel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(declaredValue, "declaredValue");
        Objects.requireNonNull(hazardClass, "hazardClass");
        Objects.requireNonNull(unNumber, "unNumber");
    }
}
