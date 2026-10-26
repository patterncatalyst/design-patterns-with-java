package com.patterncatalyst.designpatterns.ch21.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class ParcelKindsTest {

    @Test
    void standardParcelBillableWeightIsItsOwnWeight() {
        assertThat(ParcelKinds.billableWeightKg(new StandardParcel(4.0))).isEqualTo(4.0);
    }

    @Test
    void fragileParcelBillableWeightIsItsOwnWeight() {
        assertThat(ParcelKinds.billableWeightKg(new FragileParcel(2.5, "keep upright"))).isEqualTo(2.5);
    }

    @Test
    void refrigeratedParcelBillableWeightIsItsOwnWeight() {
        assertThat(ParcelKinds.billableWeightKg(new RefrigeratedParcel(8.0, -5, 4))).isEqualTo(8.0);
    }

    @Test
    void oversizedParcelBillableWeightIsDimensionalWhenThatExceedsActualWeight() {
        // (120 * 80 * 60) / 5000 = 115.2, which exceeds the 12.0kg actual weight.
        OversizedParcel parcel = new OversizedParcel(12.0, 120.0, 80.0, 60.0);

        assertThat(ParcelKinds.billableWeightKg(parcel)).isCloseTo(115.2, within(0.01));
    }

    @Test
    void oversizedParcelBillableWeightIsActualWhenThatExceedsDimensionalWeight() {
        // (20 * 20 * 20) / 5000 = 1.6, far below the 30.0kg actual weight.
        OversizedParcel parcel = new OversizedParcel(30.0, 20.0, 20.0, 20.0);

        assertThat(ParcelKinds.billableWeightKg(parcel)).isEqualTo(30.0);
    }
}
