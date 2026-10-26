package com.patterncatalyst.designpatterns.ch27.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateSettingsTest {

    @Test
    void rejectsANonPositiveSurchargeMultiplier() {
        assertThatThrownBy(() -> new RateSettings(0.0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANegativeHandlingFee() {
        assertThatThrownBy(() -> new RateSettings(1.0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
