package com.patterncatalyst.designpatterns.ch21.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class ParcelKindTest {

    @Test
    void standardParcelRejectsNonPositiveWeight() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StandardParcel(0.0));
        assertThatIllegalArgumentException().isThrownBy(() -> new StandardParcel(-1.0));
    }

    @Test
    void fragileParcelRejectsABlankHandlingNote() {
        assertThatIllegalArgumentException().isThrownBy(() -> new FragileParcel(2.0, ""));
        assertThatIllegalArgumentException().isThrownBy(() -> new FragileParcel(2.0, "   "));
        assertThatNullPointerException().isThrownBy(() -> new FragileParcel(2.0, null));
    }

    @Test
    void refrigeratedParcelRejectsAMinTempAtOrAboveMaxTemp() {
        assertThatIllegalArgumentException().isThrownBy(() -> new RefrigeratedParcel(5.0, 4, 4));
        assertThatIllegalArgumentException().isThrownBy(() -> new RefrigeratedParcel(5.0, 10, 4));
    }

    @Test
    void refrigeratedParcelAcceptsAMinTempBelowMaxTemp() {
        RefrigeratedParcel parcel = new RefrigeratedParcel(5.0, -5, 4);

        assertThat(parcel.minTempC()).isEqualTo(-5);
        assertThat(parcel.maxTempC()).isEqualTo(4);
    }

    @Test
    void oversizedParcelRejectsANonPositiveDimension() {
        assertThatIllegalArgumentException().isThrownBy(() -> new OversizedParcel(10.0, 0.0, 80.0, 60.0));
        assertThatIllegalArgumentException().isThrownBy(() -> new OversizedParcel(10.0, 120.0, -1.0, 60.0));
    }

    @Test
    void recordAccessorsAndEqualityComeForFree() {
        StandardParcel a = new StandardParcel(3.5);
        StandardParcel b = new StandardParcel(3.5);

        assertThat(a.weightKg()).isEqualTo(3.5);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("3.5");
    }
}
