package com.patterncatalyst.designpatterns.ch33.shipping;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every {@link RateStrategy} implementation satisfies the identical
 * contract: given the same {@link Shipment}, {@code quote()} returns a
 * non-negative {@link Money}. This is the fact that lets
 * {@link ShipmentBuilder}, {@link AdjustWeightCommand}, and
 * {@link SwitchRateStrategyCommand} all hold a bare {@code RateStrategy}
 * field and swap the concrete implementation behind it without any of the
 * three ever changing.
 */
class RateStrategyContractTest {

    static Stream<Arguments> strategies() {
        return Stream.of(
                Arguments.of(new WeightTieredRateStrategy("ground", 500, 180)),
                Arguments.of(new FlatRateStrategy("air", 1_999)),
                Arguments.of(new WeightTieredRateStrategy("freight", 2_400, 95)));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void everyStrategyQuotesANonNegativeAmountForTheSameShipment(RateStrategy strategy) {
        Shipment shipment = new ShipmentBuilder()
                .id("SHIP-CONTRACT")
                .carrier("GROUND")
                .destinationZone(DestinationZone.NATIONAL)
                .weightKg(12.0)
                .rateStrategy(strategy)
                .build();

        Money quote = strategy.quote(shipment);

        assertThat(quote.cents()).isGreaterThanOrEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void swappingTheStrategyOnTheSameShipmentNeverChangesWhichMethodIsCalled(RateStrategy strategy) {
        Shipment shipment = new ShipmentBuilder()
                .id("SHIP-SWAP-CONTRACT")
                .carrier("GROUND")
                .destinationZone(DestinationZone.NATIONAL)
                .weightKg(12.0)
                .build();

        new SwitchRateStrategyCommand(shipment, strategy).execute();

        assertThat(shipment.rateStrategy()).isSameAs(strategy);
        assertThat(shipment.quotedRate()).isEqualTo(strategy.quote(shipment));
    }
}
