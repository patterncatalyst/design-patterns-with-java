package com.patterncatalyst.designpatterns.ch23.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ShippingAddressTest {

    @Test
    void canonicalizesTheCountryCodeToUppercase() {
        ShippingAddress address = new ShippingAddress("500 Dock Street", "Akron", "44301", "us");

        assertThat(address.countryCode()).isEqualTo("US");
    }

    @Test
    void twoAddressesWithTheSameComponentsAreEqualAndHashIdentically() {
        ShippingAddress first = new ShippingAddress("500 Dock Street", "Akron", "44301", "US");
        ShippingAddress second = new ShippingAddress("500 Dock Street", "Akron", "44301", "us");

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void toStringReportsEveryComponent() {
        ShippingAddress address = new ShippingAddress("500 Dock Street", "Akron", "44301", "US");

        assertThat(address.toString())
                .contains("500 Dock Street")
                .contains("Akron")
                .contains("44301")
                .contains("US");
    }

    @Test
    void rejectsABlankStreet() {
        assertThatThrownBy(() -> new ShippingAddress("  ", "Akron", "44301", "US"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsACountryCodeThatIsNotTwoLetters() {
        assertThatThrownBy(() -> new ShippingAddress("500 Dock Street", "Akron", "44301", "USA"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullComponent() {
        assertThatThrownBy(() -> new ShippingAddress(null, "Akron", "44301", "US"))
                .isInstanceOf(NullPointerException.class);
    }
}
