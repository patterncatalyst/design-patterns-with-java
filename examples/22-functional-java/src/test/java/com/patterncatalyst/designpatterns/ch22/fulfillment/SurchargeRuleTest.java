package com.patterncatalyst.designpatterns.ch22.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A {@code SurchargeRule} variable cannot tell whether it holds an enum
 * constant or a lambda; this proves both satisfy the identical contract and
 * compose together regardless of which one produced them.
 */
class SurchargeRuleTest {

    private static final Shipment SHIPMENT =
            new Shipment("RETAIL-1", 2.0, DestinationZone.LOCAL, PackagingTier.FRAGILE, 1_000);

    @Test
    void anEnumConstantAndALambdaBothSatisfySurchargeRule() {
        List<SurchargeRule> rules = List.of(PackagingTier.STANDARD, PromotionalSurcharges.flatCampaignSurcharge(200));

        assertThat(rules.get(0).surchargeCents(SHIPMENT)).isZero();
        assertThat(rules.get(1).surchargeCents(SHIPMENT)).isEqualTo(200L);
    }

    @Test
    void holidayPeakSurchargeScalesWithWeight() {
        SurchargeRule peak = PromotionalSurcharges.holidayPeakSurcharge(25);

        assertThat(peak.surchargeCents(SHIPMENT)).isEqualTo(50L);
    }

    @Test
    void combinedAddsBothRulesRegardlessOfTheirSource() {
        SurchargeRule peak = PromotionalSurcharges.holidayPeakSurcharge(25);
        SurchargeRule combined = PromotionalSurcharges.combined(peak, PackagingTier.FRAGILE);

        // peak: round(2.0 * 25) = 50. FRAGILE: 350 + round(2.0 * 40) = 350 + 80 = 430.
        assertThat(combined.surchargeCents(SHIPMENT)).isEqualTo(480L);
    }
}
