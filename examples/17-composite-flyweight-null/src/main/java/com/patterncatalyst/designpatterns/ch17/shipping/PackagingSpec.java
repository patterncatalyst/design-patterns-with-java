package com.patterncatalyst.designpatterns.ch17.shipping;

import java.util.Objects;

/**
 * Intrinsic, packaging-level state shared by every parcel packed the same
 * way. A {@code STD-BOX} parcel and a thousand other {@code STD-BOX} parcels
 * all reference the identical {@link PackagingSpec} once {@link
 * PackagingSpecCatalog} has materialized it, rather than each parcel
 * carrying its own private copy of facts that never vary between them.
 */
public record PackagingSpec(String code, String material, int tareWeightGrams, HazmatClass hazmatClass) {

    public PackagingSpec {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(hazmatClass, "hazmatClass");
        if (tareWeightGrams < 0) {
            throw new IllegalArgumentException("tareWeightGrams cannot be negative: " + tareWeightGrams);
        }
    }
}
