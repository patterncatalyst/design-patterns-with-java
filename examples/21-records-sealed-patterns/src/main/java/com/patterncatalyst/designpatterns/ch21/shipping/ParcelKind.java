package com.patterncatalyst.designpatterns.ch21.shipping;

/**
 * A closed set of parcel kinds a rating engine prices and a tracking feed
 * reports on. Every kind is a transparent, immutable record; none of them
 * implements an accept() method or a visitor interface, and the compiler
 * enforces completeness over this set through the permits clause below
 * rather than through any method declared here.
 */
public sealed interface ParcelKind
        permits StandardParcel, FragileParcel, RefrigeratedParcel, OversizedParcel {

    double weightKg();
}
