package com.patterncatalyst.designpatterns.ch16.shipping.proxy;

import com.patterncatalyst.designpatterns.ch16.shipping.DestinationZone;
import com.patterncatalyst.designpatterns.ch16.shipping.LiveCarrierGateway;
import com.patterncatalyst.designpatterns.ch16.shipping.Shipment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LazyCarrierGatewayProxyTest {

    private final Shipment shipment = new Shipment(2.0, DestinationZone.REGIONAL, 12_000);

    @Test
    void realSubjectIsNotConstructedUntilTheFirstQuote() {
        int before = LiveCarrierGateway.instancesCreated();
        LazyCarrierGatewayProxy proxy = new LazyCarrierGatewayProxy("air-flat");

        assertThat(proxy.isLoaded()).isFalse();
        assertThat(LiveCarrierGateway.instancesCreated()).isEqualTo(before);

        proxy.quote(shipment);

        assertThat(proxy.isLoaded()).isTrue();
        assertThat(LiveCarrierGateway.instancesCreated()).isEqualTo(before + 1);
    }

    @Test
    void aSecondQuoteReusesTheSameRealSubjectRatherThanConstructingAnother() {
        LazyCarrierGatewayProxy proxy = new LazyCarrierGatewayProxy("air-flat");
        proxy.quote(shipment);
        int afterFirst = LiveCarrierGateway.instancesCreated();

        proxy.quote(shipment);

        assertThat(LiveCarrierGateway.instancesCreated()).isEqualTo(afterFirst);
    }

    @Test
    void forwardsToTheRealSubjectsQuote() {
        LazyCarrierGatewayProxy proxy = new LazyCarrierGatewayProxy("air-flat");

        var proxied = proxy.quote(shipment);
        var direct = new LiveCarrierGateway("air-flat").quote(shipment);

        assertThat(proxied).isEqualTo(direct);
    }
}
