package com.patterncatalyst.designpatterns.ch28.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(FlatRateStrategyProfile.class)
class RateQuoterProducerFlatProfileTest {

    @Inject
    RateQuoter rateQuoter;

    @Test
    void switchingTheConfiguredStrategySwitchesWhichQuoterTheProducerBuilds() {
        RateQuote quote = rateQuoter.quote(new Shipment("TRK-1002", 50.0, 0));

        assertThat(quote.quotedBy()).isEqualTo("flat-rate");
        assertThat(quote.amount()).isEqualTo(Money.ofCents(500));
    }
}
