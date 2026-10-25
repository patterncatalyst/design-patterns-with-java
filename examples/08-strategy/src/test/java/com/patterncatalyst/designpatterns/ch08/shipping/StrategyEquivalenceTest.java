package com.patterncatalyst.designpatterns.ch08.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import com.patterncatalyst.designpatterns.ch08.shipping.named.DistanceRateStrategy;
import com.patterncatalyst.designpatterns.ch08.shipping.named.FlatRateStrategy;
import com.patterncatalyst.designpatterns.ch08.shipping.named.WeightTieredRateStrategy;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Proves the chapter's central claim: three different dispatch mechanisms
 * (a named class, a lambda, and a {@link Carrier} enum
 * constant) quote an identical {@link Shipment} identically, because all
 * three call down into the same {@link ShippingMath}. This is a behavior
 * assertion, not a smoke test: each group below compares the actual quoted
 * amounts, not just that each strategy ran without throwing.
 */
class StrategyEquivalenceTest {

    private static final Shipment SHIPMENT = new Shipment(3.5, DestinationZone.REGIONAL, 12_000);

    static Stream<Arguments> weightTieredExpressions() {
        return Stream.of(
                Arguments.of("named", new WeightTieredRateStrategy("ground", 500, 180)),
                Arguments.of("lambda", RateStrategies.weightTiered("ground", 500, 180)),
                Arguments.of("enum", Carrier.GROUND));
    }

    @ParameterizedTest(name = "{0} weight-tiered expression")
    @MethodSource("weightTieredExpressions")
    void weightTieredExpressionsAgree(String label, RateStrategy strategy) {
        assertThat(strategy.quote(SHIPMENT)).isEqualTo(new RateQuote("ground", Money.ofCents(1130)));
    }

    static Stream<Arguments> flatExpressions() {
        return Stream.of(
                Arguments.of("named", new FlatRateStrategy("air-flat", 999)),
                Arguments.of("lambda", RateStrategies.flat("air-flat", 999)),
                Arguments.of("enum", Carrier.AIR_FLAT));
    }

    @ParameterizedTest(name = "{0} flat-rate expression")
    @MethodSource("flatExpressions")
    void flatExpressionsAgree(String label, RateStrategy strategy) {
        assertThat(strategy.quote(SHIPMENT)).isEqualTo(new RateQuote("air-flat", Money.ofCents(999)));
    }

    static Stream<Arguments> distanceZoneExpressions() {
        return Stream.of(
                Arguments.of("named", new DistanceRateStrategy("freight-zone")),
                Arguments.of("lambda", RateStrategies.distanceZone("freight-zone")),
                Arguments.of("enum", Carrier.FREIGHT_ZONE));
    }

    @ParameterizedTest(name = "{0} distance-zone expression")
    @MethodSource("distanceZoneExpressions")
    void distanceZoneExpressionsAgree(String label, RateStrategy strategy) {
        // REGIONAL = 220 cents/kg, base 400: 400 + round(220 * 3.5) = 400 + 770 = 1170.
        assertThat(strategy.quote(SHIPMENT)).isEqualTo(new RateQuote("freight-zone", Money.ofCents(1170)));
    }

    @ParameterizedTest(name = "every destination zone prices the same across expressions: {0}")
    @MethodSource("com.patterncatalyst.designpatterns.ch08.shipping.StrategyEquivalenceTest#allZones")
    void distanceZoneAgreesAcrossEveryZone(DestinationZone zone) {
        Shipment shipment = new Shipment(2.0, zone, 5_000);
        RateStrategy namedClassStrategy = new DistanceRateStrategy("freight-zone");
        RateStrategy lambda = RateStrategies.distanceZone("freight-zone");

        assertThat(namedClassStrategy.quote(shipment)).isEqualTo(lambda.quote(shipment));
        assertThat(Carrier.FREIGHT_ZONE.quote(shipment)).isEqualTo(
                new RateQuote("freight-zone", lambda.quote(shipment).amount()));
    }

    static List<DestinationZone> allZones() {
        return List.of(DestinationZone.values());
    }
}
