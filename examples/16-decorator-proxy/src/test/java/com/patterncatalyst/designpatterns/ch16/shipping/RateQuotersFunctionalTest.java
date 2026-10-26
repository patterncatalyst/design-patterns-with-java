package com.patterncatalyst.designpatterns.ch16.shipping;

import com.patterncatalyst.designpatterns.ch16.shipping.decorator.LoggingRateQuoter;
import com.patterncatalyst.designpatterns.ch16.shipping.decorator.SurchargeRateQuoter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The functional decoration factories in {@link RateQuoters} satisfy the
 * identical {@link RateQuoter} contract as the named decorator classes in
 * {@code decorator/}. This asserts the two forms agree, not just that each
 * one compiles.
 */
class RateQuotersFunctionalTest {

    private final Shipment shipment = new Shipment(2.0, DestinationZone.REGIONAL, 12_000);

    @Test
    void functionalSurchargeMatchesTheNamedClass() {
        RateQuoter core = new LiveCarrierGateway("ground");

        RateQuoter namedClass = new SurchargeRateQuoter(core, 1.5);
        RateQuoter lambda = RateQuoters.surcharging(core, 1.5);

        assertThat(lambda.quote(shipment)).isEqualTo(namedClass.quote(shipment));
    }

    @Test
    void functionalLoggingAppendsTheSameLineFormatAsTheNamedClass() {
        RateQuoter core = new LiveCarrierGateway("ground");
        List<String> classLog = new ArrayList<>();
        List<String> lambdaLog = new ArrayList<>();

        RateQuoter namedClass = new LoggingRateQuoter(core, classLog);
        RateQuoter lambda = RateQuoters.logging(core, lambdaLog);

        namedClass.quote(shipment);
        lambda.quote(shipment);

        assertThat(lambdaLog).isEqualTo(classLog);
    }

    @Test
    void functionalDecoratorsComposeWithoutANewClass() {
        RateQuoter core = new LiveCarrierGateway("ground");
        List<String> callLog = new ArrayList<>();

        RateQuoter stacked = RateQuoters.surcharging(RateQuoters.logging(core, callLog), 1.5);

        RateQuote quote = stacked.quote(shipment);

        assertThat(callLog).hasSize(1);
        assertThat(quote.amount()).isEqualTo(core.quote(shipment).amount().plus(Money.ofCents(180)));
    }
}
