package com.patterncatalyst.designpatterns.ch32.accessorials;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins LegacyAccessorialFeeCalculator's current behavior with a
 * characterization suite before anything about it changes. These numbers
 * are not a specification anyone wrote down; they are what the legacy
 * calculator actually returns today, which is the only surviving record of
 * a renegotiated fuel rate, a weight-based fee waiver, and a carrier-specific
 * oversize penalty.
 */
class LegacyAccessorialFeeCalculatorCharacterizationTest {

    private final LegacyAccessorialFeeCalculator legacy = new LegacyAccessorialFeeCalculator();

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.patterncatalyst.designpatterns.ch32.accessorials.AccessorialFeeCharacterizationCases#cases")
    @DisplayName("matches the pinned fee total for every characterization case")
    void matchesPinnedFees(String caseName, ShipmentManifest manifest, AccessorialFees expectedFees) {
        AccessorialFees actualFees = legacy.calculateFees(manifest);

        assertThat(actualFees).isEqualTo(expectedFees);
    }
}
