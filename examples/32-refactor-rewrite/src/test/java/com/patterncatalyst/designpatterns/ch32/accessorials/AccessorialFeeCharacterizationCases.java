package com.patterncatalyst.designpatterns.ch32.accessorials;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

/**
 * Five shipment profiles and the exact fee totals
 * {@link LegacyAccessorialFeeCalculator} returns today, captured by running
 * the legacy calculator, not derived from a specification that does not
 * exist anywhere else. {@link ModernAccessorialFeeCalculatorAgreementTest}
 * is judged against this same table, not against a fresh set of
 * expectations written while rebuilding the class.
 */
final class AccessorialFeeCharacterizationCases {

    private AccessorialFeeCharacterizationCases() {
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of(
                        "ocean freight, no accessorials",
                        new ShipmentManifest("SHIP-1001", "OCEAN", 100_000L, 50.0, false, false, false),
                        new AccessorialFees(15_000L, 0L, 0L, 0L)),
                Arguments.of(
                        "air freight, residential plus liftgate plus oversized all at once",
                        new ShipmentManifest("SHIP-1002", "AIR", 50_000L, 30.0, true, true, true),
                        new AccessorialFees(11_000L, 450L, 450L, 1_800L)),
                Arguments.of(
                        "ground freight, residential fee waived under the weight floor",
                        new ShipmentManifest("SHIP-1003", "GROUND", 20_000L, 10.0, true, false, false),
                        new AccessorialFees(1_600L, 0L, 0L, 0L)),
                Arguments.of(
                        "an unlisted carrier code falls back to the default fuel rate",
                        new ShipmentManifest("SHIP-1004", "RAIL", 75_000L, 40.0, false, true, false),
                        new AccessorialFees(7_500L, 0L, 350L, 0L)),
                Arguments.of(
                        "air freight oversize penalty with no liftgate",
                        new ShipmentManifest("SHIP-1005", "AIR", 10_000L, 5.0, false, false, true),
                        new AccessorialFees(2_200L, 0L, 0L, 1_800L)));
    }
}
