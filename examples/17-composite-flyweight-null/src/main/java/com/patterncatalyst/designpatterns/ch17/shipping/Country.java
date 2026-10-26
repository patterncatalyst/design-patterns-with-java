package com.patterncatalyst.designpatterns.ch17.shipping;

import java.util.Objects;

/**
 * Intrinsic, destination-level state: the facts about a country that never
 * change between one parcel and the next. Unlike {@link HazmatClass}, the
 * set of countries is not small enough to hand-enumerate as constants, so
 * {@link CountryCatalog} interns instances of this record instead.
 */
public record Country(String isoCode, String name, boolean requiresCustomsDeclaration) {

    public Country {
        Objects.requireNonNull(isoCode, "isoCode");
        Objects.requireNonNull(name, "name");
    }
}
