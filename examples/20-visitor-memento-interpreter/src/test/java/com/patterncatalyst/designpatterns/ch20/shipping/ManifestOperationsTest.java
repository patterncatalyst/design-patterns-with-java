package com.patterncatalyst.designpatterns.ch20.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ManifestOperationsTest {

    @Test
    void shippingCostForStandardParcelIsJustTheBaseRate() {
        StandardParcel parcel = new StandardParcel("A", Money.ZERO, 10.0);

        assertThat(ManifestOperations.shippingCost(parcel)).isEqualTo(Money.ofCents(500));
    }

    @Test
    void shippingCostForInternationalParcelAddsBrokerageFlatFee() {
        InternationalParcel parcel = new InternationalParcel("B", Money.ZERO, 10.0, "1234.56", "FR");

        assertThat(ManifestOperations.shippingCost(parcel)).isEqualTo(Money.ofCents(2_000));
    }

    @Test
    void shippingCostForHazardousParcelAddsPerKilogramHandlingFee() {
        HazardousParcel parcel = new HazardousParcel("C", Money.ZERO, 10.0, "Class 3", "1993");

        assertThat(ManifestOperations.shippingCost(parcel)).isEqualTo(Money.ofCents(2_500));
    }

    @Test
    void shippingCostForOversizedParcelAddsDimensionalSurcharge() {
        OversizedParcel parcel = new OversizedParcel("D", Money.ZERO, 10.0, 100.0, 100.0, 100.0);

        assertThat(ManifestOperations.shippingCost(parcel)).isEqualTo(Money.ofCents(800));
    }

    @Test
    void customsSummaryDescribesEveryEntryKindDifferently() {
        assertThat(ManifestOperations.customsSummary(new StandardParcel("A", Money.ZERO, 1.0)))
                .isEqualTo("A: domestic parcel, no customs filing required");
        assertThat(ManifestOperations.customsSummary(
                new InternationalParcel("B", Money.ZERO, 1.0, "8471.30", "DE")))
                .isEqualTo("B: customs code 8471.30, origin DE, brokerage required");
        assertThat(ManifestOperations.customsSummary(
                new HazardousParcel("C", Money.ZERO, 1.0, "Class 9", "3481")))
                .isEqualTo("C: hazmat class Class 9 (UN3481), placard required, domestic handling only");
        assertThat(ManifestOperations.customsSummary(
                new OversizedParcel("D", Money.ZERO, 1.0, 1.0, 1.0, 1.0)))
                .isEqualTo("D: oversized parcel, no customs filing, freight-desk routing required");
    }

    @Test
    void validateReturnsNoIssuesForAWellFormedStandardParcel() {
        StandardParcel parcel = new StandardParcel("A", Money.ZERO, 5.0);

        assertThat(ManifestOperations.validate(parcel)).isEmpty();
    }

    @Test
    void validateFlagsNonPositiveWeightOnAnyEntryKind() {
        StandardParcel parcel = new StandardParcel("A", Money.ZERO, 0.0);

        assertThat(ManifestOperations.validate(parcel))
                .containsExactly("A: weight must be positive, was 0.00kg");
    }

    @Test
    void validateCollectsEveryMissingFieldOnAnInternationalParcelInOnePass() {
        InternationalParcel parcel = new InternationalParcel("B", Money.ZERO, 0.0, "", "");

        assertThat(ManifestOperations.validate(parcel)).containsExactly(
                "B: weight must be positive, was 0.00kg",
                "B: customs code is required",
                "B: origin country is required");
    }

    @Test
    void validateFlagsMissingHazardFieldsOnAHazardousParcel() {
        HazardousParcel parcel = new HazardousParcel("C", Money.ZERO, 2.0, "", "");

        assertThat(ManifestOperations.validate(parcel)).containsExactly(
                "C: hazard class is required",
                "C: UN number is required");
    }

    @Test
    void validateFlagsNonPositiveDimensionsOnAnOversizedParcel() {
        OversizedParcel parcel = new OversizedParcel("D", Money.ZERO, 2.0, 0.0, 10.0, 10.0);

        assertThat(ManifestOperations.validate(parcel)).containsExactly(
                "D: all dimensions must be positive");
    }

    @Test
    void everyOperationHandlesEveryEntryKindWithoutThrowing() {
        List<ManifestEntry> manifest = List.of(
                new StandardParcel("A", Money.ZERO, 1.0),
                new InternationalParcel("B", Money.ZERO, 1.0, "1234.56", "FR"),
                new HazardousParcel("C", Money.ZERO, 1.0, "Class 3", "1993"),
                new OversizedParcel("D", Money.ZERO, 1.0, 10.0, 10.0, 10.0));

        for (ManifestEntry entry : manifest) {
            assertThat(ManifestOperations.shippingCost(entry).cents()).isGreaterThan(0);
            assertThat(ManifestOperations.customsSummary(entry)).isNotBlank();
            assertThat(ManifestOperations.validate(entry)).isEmpty();
        }
    }
}
