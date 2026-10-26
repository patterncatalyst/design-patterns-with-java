package com.patterncatalyst.designpatterns.ch20.shipping;

import java.util.Objects;

/** A parcel crossing a border, carrying the customs code and origin a broker needs. */
public record InternationalParcel(
        String id, Money declaredValue, double weightKg, String customsCode, String originCountry)
        implements ManifestEntry {

    public InternationalParcel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(declaredValue, "declaredValue");
        Objects.requireNonNull(customsCode, "customsCode");
        Objects.requireNonNull(originCountry, "originCountry");
    }
}
