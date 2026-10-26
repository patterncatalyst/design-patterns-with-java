package com.patterncatalyst.designpatterns.ch21.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RateRulesTest {

    private static final ParcelKind TWELVE_KILO_STANDARD = new StandardParcel(12.0);

    @Test
    void flatRateIgnoresWeightAndZone() {
        RateRule rule = new FlatRate(Money.ofCents(2_500));

        assertThat(RateRules.quote(rule, TWELVE_KILO_STANDARD, "EAST")).isEqualTo(Money.ofCents(2_500));
        assertThat(RateRules.quote(rule, new OversizedParcel(1.0, 5.0, 5.0, 5.0), "WEST"))
                .isEqualTo(Money.ofCents(2_500));
    }

    @Test
    void perKilogramScalesByBillableWeightNotActualWeightForAnOversizedParcel() {
        RateRule rule = new PerKilogram(Money.ofCents(100));
        // billable weight is dimensional: (120 * 80 * 60) / 5000 = 115.2kg
        OversizedParcel oversized = new OversizedParcel(12.0, 120.0, 80.0, 60.0);

        assertThat(RateRules.quote(rule, oversized, "EAST")).isEqualTo(Money.ofCents(11_520));
    }

    @Test
    void zoneSurchargeAppliesOnlyWhenTheDestinationZoneMatchesExactly() {
        RateRule rule = new ZoneSurcharge(new PerKilogram(Money.ofCents(150)), "EAST", Money.ofCents(500));

        Money withSurcharge = RateRules.quote(rule, TWELVE_KILO_STANDARD, "EAST");
        Money withoutSurcharge = RateRules.quote(rule, TWELVE_KILO_STANDARD, "WEST");

        assertThat(withSurcharge).isEqualTo(Money.ofCents(2_300));
        assertThat(withoutSurcharge).isEqualTo(Money.ofCents(1_800));
    }

    @Test
    void minimumChargeRaisesAQuoteBelowTheFloorButLeavesAHigherQuoteAlone() {
        RateRule rule = new MinimumCharge(
                new ZoneSurcharge(new PerKilogram(Money.ofCents(150)), "EAST", Money.ofCents(500)),
                Money.ofCents(2_000));

        Money aboveFloor = RateRules.quote(rule, TWELVE_KILO_STANDARD, "EAST");
        Money belowFloor = RateRules.quote(rule, TWELVE_KILO_STANDARD, "WEST");

        assertThat(aboveFloor).isEqualTo(Money.ofCents(2_300));
        assertThat(belowFloor).isEqualTo(Money.ofCents(2_000));
    }

    @Test
    void nestingTheIdenticalThreePiecesDifferentlyChangesTheQuotedResult() {
        RateRule surchargeInsideMinimum = new MinimumCharge(
                new ZoneSurcharge(new PerKilogram(Money.ofCents(150)), "EAST", Money.ofCents(2_200)),
                Money.ofCents(2_000));
        RateRule minimumInsideSurcharge = new ZoneSurcharge(
                new MinimumCharge(new PerKilogram(Money.ofCents(150)), Money.ofCents(2_000)),
                "EAST", Money.ofCents(2_200));

        Money firstQuote = RateRules.quote(surchargeInsideMinimum, TWELVE_KILO_STANDARD, "EAST");
        Money secondQuote = RateRules.quote(minimumInsideSurcharge, TWELVE_KILO_STANDARD, "EAST");

        // Both start from the identical $1.50/kg, 12kg, EAST surcharge $22.00, $20.00 floor.
        // Nested as MinimumCharge(ZoneSurcharge(...)): 18.00 + 22.00 = 40.00, already above the 20.00 floor.
        assertThat(firstQuote).isEqualTo(Money.ofCents(4_000));
        // Nested as ZoneSurcharge(MinimumCharge(...)): max(18.00, 20.00) = 20.00, then + 22.00 = 42.00.
        assertThat(secondQuote).isEqualTo(Money.ofCents(4_200));
        assertThat(firstQuote).isNotEqualTo(secondQuote);
    }

    @Test
    void describeDeconstructsThreeLevelsInOneNestedRecordPattern() {
        RateRule rule = new MinimumCharge(
                new ZoneSurcharge(new PerKilogram(Money.ofCents(150)), "EAST", Money.ofCents(500)),
                Money.ofCents(2_000));

        assertThat(RateRules.describe(rule)).isEqualTo("max($1.50/kg + $5.00 in zone EAST, floor $20.00)");
    }

    @Test
    void describeFallsBackToRecursionWhenTheShapeDoesNotMatchTheNestedCase() {
        RateRule rule = new ZoneSurcharge(new FlatRate(Money.ofCents(1_000)), "EAST", Money.ofCents(500));

        assertThat(RateRules.describe(rule)).isEqualTo("$10.00 + $5.00 in zone EAST");
    }
}
