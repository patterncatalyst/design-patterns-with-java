package com.patterncatalyst.designpatterns.ch23.shipping;

import java.util.Locale;
import java.util.Objects;

/**
 * A validated shipping address. Everything a hand-written value class used
 * to need written out by hand, a private final field per component, a
 * constructor that assigns each one, {@code equals}, {@code hashCode}, and
 * {@code toString}, the compiler generates from this one declaration. The
 * compact canonical constructor is the only code an author still writes,
 * and it carries the validation and the one piece of canonicalization
 * (the country code's case) this type needs.
 */
public record ShippingAddress(String street, String city, String postalCode, String countryCode) {

    public ShippingAddress {
        Objects.requireNonNull(street, "street");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(postalCode, "postalCode");
        Objects.requireNonNull(countryCode, "countryCode");
        if (street.isBlank()) {
            throw new IllegalArgumentException("street cannot be blank");
        }
        if (city.isBlank()) {
            throw new IllegalArgumentException("city cannot be blank");
        }
        if (countryCode.length() != 2) {
            throw new IllegalArgumentException(
                    "countryCode must be an ISO 3166-1 alpha-2 code: " + countryCode);
        }
        countryCode = countryCode.toUpperCase(Locale.ROOT);
    }
}
