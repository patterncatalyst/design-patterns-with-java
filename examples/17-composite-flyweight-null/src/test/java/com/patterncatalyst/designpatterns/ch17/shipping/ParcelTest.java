package com.patterncatalyst.designpatterns.ch17.shipping;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class ParcelTest {

    private static final PackagingSpec STD_BOX =
            new PackagingSpec("STD-BOX", "corrugated cardboard", 180, HazmatClass.NONE);
    private static final Country US = new Country("US", "United States", false);

    @Test
    void rejectsANegativeWeight() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Parcel("P-1", -1, 100, STD_BOX, US));
    }

    @Test
    void rejectsANegativeValue() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Parcel("P-1", 100, -1, STD_BOX, US));
    }

    @Test
    void rejectsAMissingPackagingSpec() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Parcel("P-1", 100, 100, null, US));
    }

    @Test
    void rejectsAMissingDestination() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Parcel("P-1", 100, 100, STD_BOX, null));
    }
}
