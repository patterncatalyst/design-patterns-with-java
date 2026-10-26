package com.patterncatalyst.designpatterns.ch20.shipping;

/**
 * A fixed set of manifest line-item kinds a checkout-to-carrier export
 * manifest can hold. The set is closed on purpose: a new carrier
 * requirement shows up as a new operation over these four kinds far more
 * often than it shows up as a fifth kind, which is exactly the shape
 * Visitor answers. Every operation on this hierarchy lives outside these
 * record types, as a static method dispatching on an exhaustive switch,
 * with no accept() method declared anywhere in this file or its
 * implementations.
 */
public sealed interface ManifestEntry
        permits StandardParcel, InternationalParcel, HazardousParcel, OversizedParcel {

    String id();

    Money declaredValue();

    double weightKg();
}
