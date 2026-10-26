package com.patterncatalyst.designpatterns.ch28.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RateQuoterProducerTest {

    @Inject
    RateQuoter rateQuoter;

    @Test
    void producerYieldsTheQuoterTheConfiguredStrategySelects() {
        // rateQuoter is a client proxy for the RateQuoter interface; asking
        // it to quote is how a caller observes which concrete
        // implementation RateQuoterProducer built, the same way any other
        // injected bean is observed through its contract rather than its
        // runtime class.
        RateQuote quote = rateQuoter.quote(new Shipment("TRK-1001", 4.0, 0));

        assertThat(quote.quotedBy()).isEqualTo("weight-tiered");
        assertThat(quote.amount()).isEqualTo(Money.ofCents(500 + Math.round(180 * 4.0)));
    }
}
