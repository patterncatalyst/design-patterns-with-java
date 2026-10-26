package com.patterncatalyst.designpatterns.ch17.shipping;

import java.util.Objects;

/**
 * The Composite leaf: a single line item. {@code packagingSpec} and {@code
 * destination} are both flyweights, shared with every other parcel packed
 * and shipped the same way; {@code weightGrams} and {@code valueCents} are
 * this parcel's own, never shared, extrinsic state.
 */
public record Parcel(String id, int weightGrams, long valueCents, PackagingSpec packagingSpec, Country destination)
        implements ShipmentNode {

    public Parcel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(packagingSpec, "packagingSpec");
        Objects.requireNonNull(destination, "destination");
        if (weightGrams < 0) {
            throw new IllegalArgumentException("weightGrams cannot be negative: " + weightGrams);
        }
        if (valueCents < 0) {
            throw new IllegalArgumentException("valueCents cannot be negative: " + valueCents);
        }
    }
}
