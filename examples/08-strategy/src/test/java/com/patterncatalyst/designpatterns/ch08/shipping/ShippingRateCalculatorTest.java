package com.patterncatalyst.designpatterns.ch08.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.patterncatalyst.designpatterns.ch08.shipping.named.DistanceRateStrategy;
import com.patterncatalyst.designpatterns.ch08.shipping.named.WeightTieredRateStrategy;
import org.junit.jupiter.api.Test;

/**
 * {@link ShippingRateCalculator} is the caller side of this chapter's
 * Strategy: it must quote through whichever {@link RateStrategy} it
 * currently holds, and reassigning that strategy with
 * {@link ShippingRateCalculator#useStrategy(RateStrategy)} must change every
 * quote from that point on without the caller itself changing.
 */
class ShippingRateCalculatorTest {

    private static final Shipment SHIPMENT = new Shipment(3.5, DestinationZone.REGIONAL, 12_000);

    @Test
    void quotesThroughTheInjectedStrategy() {
        ShippingRateCalculator calculator =
                new ShippingRateCalculator(new WeightTieredRateStrategy("ground", 500, 180));

        assertThat(calculator.quote(SHIPMENT)).isEqualTo(new RateQuote("ground", Money.ofCents(1130)));
    }

    @Test
    void swappingTheStrategyChangesSubsequentQuotesOnly() {
        ShippingRateCalculator calculator =
                new ShippingRateCalculator(new WeightTieredRateStrategy("ground", 500, 180));
        RateQuote before = calculator.quote(SHIPMENT);

        calculator.useStrategy(new DistanceRateStrategy("freight-zone"));
        RateQuote after = calculator.quote(SHIPMENT);

        assertThat(before).isEqualTo(new RateQuote("ground", Money.ofCents(1130)));
        assertThat(after).isEqualTo(new RateQuote("freight-zone", Money.ofCents(1170)));
    }

    @Test
    void acceptsALambdaInterchangeablyWithANamedClassStrategy() {
        ShippingRateCalculator calculator = new ShippingRateCalculator(
                RateStrategies.weightTiered("ground", 500, 180));

        assertThat(calculator.quote(SHIPMENT)).isEqualTo(new RateQuote("ground", Money.ofCents(1130)));
    }

    @Test
    void acceptsACarrierEnumConstantInterchangeablyToo() {
        ShippingRateCalculator calculator = new ShippingRateCalculator(Carrier.AIR_FLAT);

        assertThat(calculator.quote(SHIPMENT)).isEqualTo(new RateQuote("air-flat", Money.ofCents(999)));
    }

    @Test
    void rejectsANullStrategyAtConstructionAndAtSwap() {
        assertThatThrownBy(() -> new ShippingRateCalculator(null))
                .isInstanceOf(NullPointerException.class);

        ShippingRateCalculator calculator = new ShippingRateCalculator(Carrier.AIR_FLAT);
        assertThatThrownBy(() -> calculator.useStrategy(null))
                .isInstanceOf(NullPointerException.class);
    }
}
