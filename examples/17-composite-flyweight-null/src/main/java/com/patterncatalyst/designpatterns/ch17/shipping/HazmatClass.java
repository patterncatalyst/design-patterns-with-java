package com.patterncatalyst.designpatterns.ch17.shipping;

/**
 * A closed set of hazard classifications a parcel's packaging might carry.
 * An enum constant is already a Flyweight the language hands you for free:
 * {@code HazmatClass.LITHIUM} is one JVM instance no matter how many
 * {@link PackagingSpec} records reference it, with no catalog or pool
 * needed to make that true.
 */
public enum HazmatClass {
    NONE,
    FLAMMABLE,
    CORROSIVE,
    LITHIUM
}
