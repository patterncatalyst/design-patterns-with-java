package com.patterncatalyst.designpatterns.ch16.shipping.proxy;

import com.patterncatalyst.designpatterns.ch16.shipping.ClearanceLevel;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises the {@code java.lang.reflect.Proxy}-backed access-control
 * proxy: a sufficiently cleared caller gets back exactly what the real
 * subject would have returned, an under-cleared caller never reaches the
 * real subject at all, and every intercepted call is counted either way.
 */
@ExtendWith(MockitoExtension.class)
class RateQuoterAccessControlTest {

    @Mock
    private RateQuoter real;

    private final Shipment shipment = new Shipment(2.0, DestinationZone.REGIONAL, 12_000);

    @Test
    void sufficientClearanceForwardsAndReturnsTheRealResultUnchanged() {
        RateQuote quote = new RateQuote("freight-zone", Money.ofCents(740));
        when(real.quote(shipment)).thenReturn(quote);
        RateQuoter proxy = RateQuoterAccessControl.guarded(real, ClearanceLevel.WAREHOUSE, ClearanceLevel.WAREHOUSE);

        RateQuote result = proxy.quote(shipment);

        assertThat(result).isEqualTo(quote);
        verify(real).quote(shipment);
        assertThat(RateQuoterAccessControl.callCount(proxy)).isEqualTo(1);
    }

    @Test
    void insufficientClearanceIsDeniedBeforeTheRealSubjectEverSeesTheCall() {
        RateQuoter proxy = RateQuoterAccessControl.guarded(real, ClearanceLevel.GUEST, ClearanceLevel.WAREHOUSE);

        assertThatThrownBy(() -> proxy.quote(shipment))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("WAREHOUSE")
                .hasMessageContaining("GUEST");

        verify(real, never()).quote(shipment);
        assertThat(RateQuoterAccessControl.callCount(proxy)).isEqualTo(1);
    }

    @Test
    void adminClearanceAlsoSatisfiesAWarehouseRequirement() {
        when(real.quote(shipment)).thenReturn(new RateQuote("freight-zone", Money.ofCents(740)));
        RateQuoter proxy = RateQuoterAccessControl.guarded(real, ClearanceLevel.ADMIN, ClearanceLevel.WAREHOUSE);

        proxy.quote(shipment);

        verify(real).quote(shipment);
    }
}
