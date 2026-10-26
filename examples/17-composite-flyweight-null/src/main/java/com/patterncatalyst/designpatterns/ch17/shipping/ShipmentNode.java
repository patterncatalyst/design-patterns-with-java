package com.patterncatalyst.designpatterns.ch17.shipping;

/**
 * The Composite contract: a single {@link Parcel} and a {@link ShipmentBox}
 * holding an entire sub-tree both answer the same questions the same way.
 * Sealing the interface to exactly these two permitted types is what lets
 * {@link ShipmentNodes} dispatch on it with an exhaustive pattern-matching
 * {@code switch} and no {@code default} branch for the compiler to leave
 * unchecked.
 */
public sealed interface ShipmentNode permits Parcel, ShipmentBox {
}
