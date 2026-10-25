package com.patterncatalyst.designpatterns.ch08.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@link RateStrategies#withInsuranceSurcharge} is the capability this
 * chapter credits to lambdas as values: composing a new strategy from an
 * existing one without a decorator class. These tests verify the composed
 * quote's actual amount, not merely that composition compiles.
 */
class RateStrategiesCompositionTest {

    @Test
    void surchargeIsAddedOnTopOfTheBaseQuote() {
        Shipment shipment = new Shipment(3.5, DestinationZone.REGIONAL, 12_000);
        RateStrategy base = RateStrategies.weightTiered("ground", 500, 180);
        RateStrategy insured = RateStrategies.withInsuranceSurcharge(base, 0.80);

        // base = 1130 cents; declared value $120.00 * 0.80 cents/dollar = 96 cents surcharge.
        assertThat(insured.quote(shipment)).isEqualTo(new RateQuote("ground", Money.ofCents(1226)));
    }

    @Test
    void zeroDeclaredValueAddsNoSurcharge() {
        Shipment shipment = new Shipment(3.5, DestinationZone.REGIONAL, 0);
        RateStrategy base = RateStrategies.flat("air-flat", 999);
        RateStrategy insured = RateStrategies.withInsuranceSurcharge(base, 0.80);

        assertThat(insured.quote(shipment)).isEqualTo(new RateQuote("air-flat", Money.ofCents(999)));
    }

    @Test
    void theBaseStrategyIsUnaffectedByComposition() {
        Shipment shipment = new Shipment(3.5, DestinationZone.REGIONAL, 12_000);
        RateStrategy base = RateStrategies.weightTiered("ground", 500, 180);
        RateStrategies.withInsuranceSurcharge(base, 0.80);

        assertThat(base.quote(shipment)).isEqualTo(new RateQuote("ground", Money.ofCents(1130)));
    }

    @Test
    void surchargesCanBeStackedByComposingTwice() {
        Shipment shipment = new Shipment(3.5, DestinationZone.REGIONAL, 12_000);
        RateStrategy base = RateStrategies.flat("air-flat", 999);
        RateStrategy doublyInsured = RateStrategies.withInsuranceSurcharge(
                RateStrategies.withInsuranceSurcharge(base, 0.50), 0.30);

        // base 999 + (120.00 * 0.50 = 60) + (120.00 * 0.30 = 36) = 1095.
        assertThat(doublyInsured.quote(shipment)).isEqualTo(new RateQuote("air-flat", Money.ofCents(1095)));
    }
}
