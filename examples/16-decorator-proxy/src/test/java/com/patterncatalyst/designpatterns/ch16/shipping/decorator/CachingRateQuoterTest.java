package com.patterncatalyst.designpatterns.ch16.shipping.decorator;

import com.patterncatalyst.designpatterns.ch16.shipping.DestinationZone;
import com.patterncatalyst.designpatterns.ch16.shipping.Money;
import com.patterncatalyst.designpatterns.ch16.shipping.RateQuote;
import com.patterncatalyst.designpatterns.ch16.shipping.RateQuoter;
import com.patterncatalyst.designpatterns.ch16.shipping.Shipment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachingRateQuoterTest {

    @Mock
    private RateQuoter inner;

    private final Shipment shipment = new Shipment(2.0, DestinationZone.REGIONAL, 12_000);

    @Test
    void secondLookupForTheSameShipmentNeverReachesTheWrappedQuoter() {
        RateQuote quote = new RateQuote("ground", Money.ofCents(740));
        when(inner.quote(shipment)).thenReturn(quote);
        CachingRateQuoter caching = new CachingRateQuoter(inner);

        RateQuote first = caching.quote(shipment);
        RateQuote second = caching.quote(shipment);

        assertThat(first).isEqualTo(quote);
        assertThat(second).isEqualTo(quote);
        verify(inner, times(1)).quote(shipment);
        assertThat(caching.missCount()).isEqualTo(1);
        assertThat(caching.hitCount()).isEqualTo(1);
    }

    @Test
    void aDifferentShipmentIsAlwaysAMiss() {
        Shipment other = new Shipment(5.0, DestinationZone.NATIONAL, 0);
        when(inner.quote(shipment)).thenReturn(new RateQuote("ground", Money.ofCents(740)));
        when(inner.quote(other)).thenReturn(new RateQuote("ground", Money.ofCents(1_700)));
        CachingRateQuoter caching = new CachingRateQuoter(inner);

        caching.quote(shipment);
        caching.quote(other);

        verify(inner, times(1)).quote(shipment);
        verify(inner, times(1)).quote(other);
        assertThat(caching.missCount()).isEqualTo(2);
        assertThat(caching.hitCount()).isZero();
    }
}
