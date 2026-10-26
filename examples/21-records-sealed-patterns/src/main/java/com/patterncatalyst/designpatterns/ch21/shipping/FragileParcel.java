package com.patterncatalyst.designpatterns.ch21.shipping;

import java.util.Objects;

/** A parcel that carries a handling note a warehouse worker must read before moving it. */
public record FragileParcel(double weightKg, String handlingNote) implements ParcelKind {

    public FragileParcel {
        if (weightKg <= 0.0) {
            throw new IllegalArgumentException("weightKg must be positive, was " + weightKg);
        }
        Objects.requireNonNull(handlingNote, "handlingNote");
        if (handlingNote.isBlank()) {
            throw new IllegalArgumentException("handlingNote must not be blank");
        }
    }
}
