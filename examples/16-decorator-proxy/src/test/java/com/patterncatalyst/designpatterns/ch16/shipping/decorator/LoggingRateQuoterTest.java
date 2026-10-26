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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingRateQuoterTest {

    @Mock
    private RateQuoter inner;

    @Test
    void appendsOneLineAndReturnsTheWrappedResultUnchanged() {
        Shipment shipment = new Shipment(2.0, DestinationZone.REGIONAL, 12_000);
        RateQuote quote = new RateQuote("ground", Money.ofCents(740));
        when(inner.quote(shipment)).thenReturn(quote);
        List<String> callLog = new ArrayList<>();
        LoggingRateQuoter logging = new LoggingRateQuoter(inner, callLog);

        RateQuote result = logging.quote(shipment);

        assertThat(result).isEqualTo(quote);
        assertThat(callLog).hasSize(1);
        assertThat(callLog.getFirst()).contains("REGIONAL").contains("$7.40");
        verify(inner, times(1)).quote(shipment);
    }
}
