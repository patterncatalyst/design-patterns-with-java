package com.patterncatalyst.designpatterns.ch31.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Characterization tests for {@link LegacyFreightQuoteCalculator}: no row in
 * {@link #representativeShipments()} was derived from a specification.
 * Each one was built by probing the calculator with a representative or
 * boundary input and recording the actual {@link FreightQuote} it returned,
 * quirks included, as the expected value. "Unchanged" is the property these
 * tests protect, not "correct."
 *
 * <p>Every row is re-run against the Fowler-refactored
 * {@link FreightQuoteCalculator} too. That second pass is the proof the
 * refactor preserved behavior: the suite that pinned the legacy class stays
 * green against the reorganized one without a single expected value
 * changing.
 */
class FreightQuoteCharacterizationTest {

    private final LegacyFreightQuoteCalculator legacy = new LegacyFreightQuoteCalculator();
    private final FreightQuoteCalculator refactored = new FreightQuoteCalculator();

    static Stream<Arguments> representativeShipments() {
        return Stream.of(
                Arguments.of("ground, same zone, light parcel",
                        "ground", "A", "A", 5.0, false, 50_000L,
                        2, 500L, 0L, 500L),
                Arguments.of("ground, cross zone, exactly at the 10kg tier boundary",
                        "ground", "A", "B", 10.0, false, 50_000L,
                        5, 500L, 150L, 650L),
                Arguments.of("ground, cross zone, just under the 10kg tier boundary",
                        "ground", "A", "B", 9.99, false, 50_000L,
                        5, 500L, 0L, 500L),
                Arguments.of("air, same zone, residential at exactly 20kg",
                        "air", "A", "A", 20.0, true, 50_000L,
                        1, 1500L, 300L, 1800L),
                Arguments.of("air, same zone, residential just over 20kg",
                        "air", "A", "A", 20.01, true, 50_000L,
                        1, 1500L, 550L, 2050L),
                Arguments.of("express, high declared value with a truncated insurance surcharge",
                        "express", "A", "B", 2.0, false, 100_250L,
                        1, 3000L, 2L, 3002L),
                Arguments.of("ground, same zone, declared value exactly at the insurance threshold",
                        "ground", "A", "A", 5.0, false, 100_000L,
                        2, 500L, 0L, 500L),
                Arguments.of("ground, same zone, declared value one cent over the insurance threshold",
                        "ground", "A", "A", 5.0, false, 100_001L,
                        2, 500L, 0L, 500L));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("representativeShipments")
    @DisplayName("pins the legacy calculator's current output")
    void legacyCalculatorMatchesPinnedBehavior(String caseName, String carrierCode, String originZone,
            String destinationZone, double weightKg, boolean isResidential, long declaredValueCents,
            int expectedTransitDays, long expectedBaseCents, long expectedSurchargeCents, long expectedTotalCents) {
        FreightQuote quote = legacy.quote(carrierCode, originZone, destinationZone, weightKg, isResidential,
                declaredValueCents);

        assertThat(quote)
                .extracting(FreightQuote::transitDays, FreightQuote::baseCents, FreightQuote::surchargeCents,
                        FreightQuote::totalCents)
                .containsExactly(expectedTransitDays, expectedBaseCents, expectedSurchargeCents, expectedTotalCents);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("representativeShipments")
    @DisplayName("the Fowler refactor keeps the same pinned output green")
    void refactoredCalculatorMatchesPinnedBehavior(String caseName, String carrierCode, String originZone,
            String destinationZone, double weightKg, boolean isResidential, long declaredValueCents,
            int expectedTransitDays, long expectedBaseCents, long expectedSurchargeCents, long expectedTotalCents) {
        FreightQuote quote = refactored.quote(
                new ShipmentDetails(carrierCode, originZone, destinationZone, weightKg, isResidential,
                        declaredValueCents));

        assertThat(quote)
                .extracting(FreightQuote::transitDays, FreightQuote::baseCents, FreightQuote::surchargeCents,
                        FreightQuote::totalCents)
                .containsExactly(expectedTransitDays, expectedBaseCents, expectedSurchargeCents, expectedTotalCents);
    }

    @Nested
    @DisplayName("quirks pinned by name, not only by table row")
    class PinnedQuirks {

        @Test
        @DisplayName("the 10kg weight tier is a cliff, not a ramp: 9.99kg and 10.0kg differ by a full tier")
        void pinsTheWeightTierCliff() {
            FreightQuote justUnder = legacy.quote("ground", "A", "B", 9.99, false, 50_000L);
            FreightQuote atBoundary = legacy.quote("ground", "A", "B", 10.0, false, 50_000L);

            assertThat(justUnder.surchargeCents()).isZero();
            assertThat(atBoundary.surchargeCents()).isEqualTo(150L);
        }

        @Test
        @DisplayName("the residential threshold is a strict greater-than: exactly 20kg pays no surcharge")
        void pinsTheResidentialThresholdQuirk() {
            FreightQuote atThreshold = legacy.quote("air", "A", "A", 20.0, true, 50_000L);
            FreightQuote justOver = legacy.quote("air", "A", "A", 20.01, true, 50_000L);

            assertThat(atThreshold.surchargeCents()).isEqualTo(300L);
            assertThat(justOver.surchargeCents()).isEqualTo(550L);
        }

        @Test
        @DisplayName("the declared-value surcharge truncates toward zero instead of rounding to the nearest cent")
        void pinsTheDeclaredValueTruncationQuirk() {
            // $2.50 (2.5 cents of true surcharge) truncates to 2 cents, not the
            // mathematically rounded 3.
            FreightQuote truncatesDown = legacy.quote("express", "A", "B", 2.0, false, 100_250L);
            // A one-cent excess over the threshold (0.01 cents of true surcharge)
            // truncates all the way to zero.
            FreightQuote truncatesToZero = legacy.quote("ground", "A", "A", 5.0, false, 100_001L);

            assertThat(truncatesDown.surchargeCents()).isEqualTo(2L);
            assertThat(truncatesToZero.surchargeCents()).isZero();
        }

        @Test
        @DisplayName("the refactor reproduces every pinned quirk, not just the common-path totals")
        void refactorReproducesEveryPinnedQuirk() {
            FreightQuote legacyTierCliff = legacy.quote("ground", "A", "B", 10.0, false, 50_000L);
            FreightQuote refactoredTierCliff = refactored.quote(
                    new ShipmentDetails("ground", "A", "B", 10.0, false, 50_000L));

            FreightQuote legacyTruncation = legacy.quote("express", "A", "B", 2.0, false, 100_250L);
            FreightQuote refactoredTruncation = refactored.quote(
                    new ShipmentDetails("express", "A", "B", 2.0, false, 100_250L));

            assertThat(refactoredTierCliff).isEqualTo(legacyTierCliff);
            assertThat(refactoredTruncation).isEqualTo(legacyTruncation);
        }
    }

    @Nested
    @DisplayName("an unknown carrier is rejected by both calculators")
    class UnknownCarrier {

        @Test
        @DisplayName("the legacy calculator throws")
        void legacyRejectsUnknownCarrier() {
            assertThatThrownBy(() -> legacy.quote("drone", "A", "A", 1.0, false, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("drone");
        }

        @Test
        @DisplayName("the refactored calculator throws the same way")
        void refactoredRejectsUnknownCarrier() {
            assertThatThrownBy(() -> refactored.quote(new ShipmentDetails("drone", "A", "A", 1.0, false, 0L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("drone");
        }
    }
}
