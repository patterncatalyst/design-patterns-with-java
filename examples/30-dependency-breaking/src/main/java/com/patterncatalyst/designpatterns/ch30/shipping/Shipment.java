package com.patterncatalyst.designpatterns.ch30.shipping;

import java.util.Objects;

/**
 * A shipment awaiting dispatch. Equality is structural, which lets tests
 * compare a dispatch result's inputs without hand-written equals/hashCode.
 */
public record Shipment(String id, long baseCostCents) {

    public Shipment {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        if (baseCostCents < 0) {
            throw new IllegalArgumentException("baseCostCents cannot be negative: " + baseCostCents);
        }
    }
}
