package com.patterncatalyst.designpatterns.ch32.accessorials;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ModernAccessorialFeeCalculator earns the right to take traffic by passing
 * the same characterization table LegacyAccessorialFeeCalculator is pinned
 * against, not by looking cleaner. A second, direct engine-to-engine check
 * proves the two calculators still agree on a case outside that table,
 * independent of either one's own pinned expectations.
 */
class ModernAccessorialFeeCalculatorAgreementTest {

    private final ModernAccessorialFeeCalculator modern = new ModernAccessorialFeeCalculator();
    private final LegacyAccessorialFeeCalculator legacy = new LegacyAccessorialFeeCalculator();

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.patterncatalyst.designpatterns.ch32.accessorials.AccessorialFeeCharacterizationCases#cases")
    @DisplayName("matches the same pinned fee total the legacy calculator is held to")
    void matchesPinnedFees(String caseName, ShipmentManifest manifest, AccessorialFees expectedFees) {
        AccessorialFees actualFees = modern.calculateFees(manifest);

        assertThat(actualFees).isEqualTo(expectedFees);
    }

    @Test
    @DisplayName("agrees with the legacy calculator directly, on a case outside the pinned table")
    void agreesWithLegacyOnAnUncoveredCase() {
        ShipmentManifest manifest = new ShipmentManifest("SHIP-9001", "OCEAN", 42_000L, 18.0, true, true, true);

        assertThat(modern.calculateFees(manifest)).isEqualTo(legacy.calculateFees(manifest));
    }
}
