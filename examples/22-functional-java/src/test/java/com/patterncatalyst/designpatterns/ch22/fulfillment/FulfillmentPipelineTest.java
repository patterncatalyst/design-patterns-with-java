package com.patterncatalyst.designpatterns.ch22.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link FulfillmentPipeline#process} runs the identical validate-price-assign
 * sequence no matter which lambdas a caller supplies for the two steps that
 * vary; this asserts both the accepted and rejected outcomes for two
 * different channels, each wired up with its own pair of lambdas.
 */
class FulfillmentPipelineTest {

    private final ZoneRateLookup rates = new ZoneRateLookup(Map.of(
            DestinationZone.REGIONAL, Money.ofCents(650),
            DestinationZone.NATIONAL, Money.ofCents(1_200)));

    @Test
    void acceptsAValidRetailShipmentAndAssignsAManifest() {
        Shipment shipment = new Shipment("RETAIL-1", 2.0, DestinationZone.REGIONAL, PackagingTier.STANDARD, 8_000);

        FulfillmentRecord outcome = FulfillmentPipeline.process(
                shipment, FulfillmentRules::retailValidate, s -> FulfillmentRules.retailPrice(rates, s));

        assertThat(outcome.accepted()).isTrue();
        assertThat(outcome.manifestId()).isNotBlank();
        assertThat(outcome.total()).isEqualTo(Money.ofCents(650));
        assertThat(outcome.rejectionReason()).isEmpty();
    }

    @Test
    void rejectsARetailShipmentOverTheDeclaredValueCapWithNoManifestAssigned() {
        Shipment shipment = new Shipment("RETAIL-2", 1.0, DestinationZone.REGIONAL, PackagingTier.STANDARD, 60_000);

        FulfillmentRecord outcome = FulfillmentPipeline.process(
                shipment, FulfillmentRules::retailValidate, s -> FulfillmentRules.retailPrice(rates, s));

        assertThat(outcome.accepted()).isFalse();
        assertThat(outcome.manifestId()).isEmpty();
        assertThat(outcome.total()).isEqualTo(Money.ZERO);
        assertThat(outcome.rejectionReason()).isNotBlank();
    }

    @Test
    void acceptsAValidWholesaleShipmentThroughADifferentPairOfSteps() {
        Shipment shipment = new Shipment("WHOLESALE-1", 35.0, DestinationZone.NATIONAL, PackagingTier.OVERSIZED, 1_000);

        FulfillmentRecord outcome = FulfillmentPipeline.process(
                shipment, FulfillmentRules::wholesaleValidate, s -> FulfillmentRules.wholesalePrice(rates, s));

        assertThat(outcome.accepted()).isTrue();
        assertThat(outcome.total()).isEqualTo(Money.ofCents(4_545));
    }

    @Test
    void rejectsAWholesaleShipmentUnderTheMinimumWeight() {
        Shipment shipment = new Shipment("WHOLESALE-2", 5.0, DestinationZone.NATIONAL, PackagingTier.STANDARD, 1_000);

        FulfillmentRecord outcome = FulfillmentPipeline.process(
                shipment, FulfillmentRules::wholesaleValidate, s -> FulfillmentRules.wholesalePrice(rates, s));

        assertThat(outcome.accepted()).isFalse();
    }

    @Test
    void eachAcceptedShipmentGetsItsOwnIncreasingManifestNumber() {
        Shipment first = new Shipment("RETAIL-3", 1.0, DestinationZone.REGIONAL, PackagingTier.STANDARD, 1_000);
        Shipment second = new Shipment("RETAIL-4", 1.0, DestinationZone.REGIONAL, PackagingTier.STANDARD, 1_000);

        FulfillmentRecord firstOutcome = FulfillmentPipeline.process(
                first, FulfillmentRules::retailValidate, s -> FulfillmentRules.retailPrice(rates, s));
        FulfillmentRecord secondOutcome = FulfillmentPipeline.process(
                second, FulfillmentRules::retailValidate, s -> FulfillmentRules.retailPrice(rates, s));

        assertThat(firstOutcome.manifestId()).isNotEqualTo(secondOutcome.manifestId());
    }
}
