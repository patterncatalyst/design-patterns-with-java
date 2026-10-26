package com.patterncatalyst.designpatterns.ch16.shipping;

import com.patterncatalyst.designpatterns.ch16.shipping.decorator.CachingRateQuoter;
import com.patterncatalyst.designpatterns.ch16.shipping.decorator.LoggingRateQuoter;
import com.patterncatalyst.designpatterns.ch16.shipping.decorator.SurchargeRateQuoter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves stacking order is part of the design, not an implementation
 * detail: the same three decorator classes, constructed with the same
 * arguments, produce a different observable result depending on which one
 * wraps which.
 */
class DecoratorOrderingTest {

    private final Shipment shipment = new Shipment(2.0, DestinationZone.REGIONAL, 12_000);

    @Test
    void surchargeOutsideCacheRecomputesTheMarkupOnEveryCall() {
        List<String> callLog = new ArrayList<>();
        LoggingRateQuoter logging = new LoggingRateQuoter(new LiveCarrierGateway("ground"), callLog);
        CachingRateQuoter caching = new CachingRateQuoter(logging);
        SurchargeRateQuoter surcharge = new SurchargeRateQuoter(caching, 1.5);

        RateQuote first = surcharge.quote(shipment);
        surcharge.setCentsPerDeclaredDollar(3.0);
        RateQuote second = surcharge.quote(shipment);

        assertThat(second.amount()).isNotEqualTo(first.amount());
        assertThat(caching.missCount()).isEqualTo(1);
        assertThat(caching.hitCount()).isEqualTo(1);
        assertThat(callLog).hasSize(1);
    }

    @Test
    void cacheOutsideSurchargeMemoizesTheAlreadySurchargedTotal() {
        List<String> callLog = new ArrayList<>();
        LoggingRateQuoter logging = new LoggingRateQuoter(new LiveCarrierGateway("ground"), callLog);
        SurchargeRateQuoter surcharge = new SurchargeRateQuoter(logging, 1.5);
        CachingRateQuoter caching = new CachingRateQuoter(surcharge);

        RateQuote first = caching.quote(shipment);
        surcharge.setCentsPerDeclaredDollar(3.0);
        RateQuote second = caching.quote(shipment);

        assertThat(second.amount()).isEqualTo(first.amount());
        assertThat(caching.missCount()).isEqualTo(1);
        assertThat(caching.hitCount()).isEqualTo(1);
        assertThat(callLog).hasSize(1);
    }

    @Test
    void sameShipmentPricesIdenticallyRegardlessOfStackDepth() {
        RateQuoter core = new LiveCarrierGateway("ground");
        RateQuoter wrapped = new SurchargeRateQuoter(
                new CachingRateQuoter(new LoggingRateQuoter(core, new ArrayList<>())), 1.5);

        assertThat(wrapped.quote(shipment).amount()).isEqualTo(core.quote(shipment).amount().plus(Money.ofCents(180)));
    }
}
