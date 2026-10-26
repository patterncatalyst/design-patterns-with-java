package com.patterncatalyst.designpatterns.ch24.shipping;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavior that does not depend on concurrency timing: the fan-out returns
 * the gateway's own computed quote for every carrier, and a single carrier
 * failure surfaces as a {@link CarrierQuoteException} rather than a bare
 * {@link java.util.concurrent.ExecutionException}.
 */
class RateQuoteFanOutServiceTest {

    @Test
    void returnsOneQuotePerCarrier() throws InterruptedException {
        CarrierGateway gateway = (shipment, carrierId) ->
                new RateQuote(carrierId, carrierId.length() * 100L + Math.round(shipment.weightKg()));
        RateQuoteFanOutService service = new RateQuoteFanOutService(gateway);
        Shipment shipment = new Shipment("SHIP-1", 5.0);

        List<RateQuote> quotes = service.fanOut(shipment, List.of("acme", "bolt", "crate"));

        assertThat(quotes).containsExactlyInAnyOrder(
                new RateQuote("acme", 405L),
                new RateQuote("bolt", 405L),
                new RateQuote("crate", 505L));
    }

    @Test
    void wrapsAFailedCarrierCallInACarrierQuoteException() {
        CarrierGateway gateway = (shipment, carrierId) -> {
            if (carrierId.equals("bolt")) {
                throw new IllegalStateException("bolt rate API is down");
            }
            return new RateQuote(carrierId, 100L);
        };
        RateQuoteFanOutService service = new RateQuoteFanOutService(gateway);
        Shipment shipment = new Shipment("SHIP-2", 1.0);

        assertThatThrownBy(() -> service.fanOut(shipment, List.of("acme", "bolt")))
                .isInstanceOf(CarrierQuoteException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsANonPositiveConcurrencyLimit() {
        CarrierGateway gateway = (shipment, carrierId) -> new RateQuote(carrierId, 1L);
        RateQuoteFanOutService service = new RateQuoteFanOutService(gateway);
        Shipment shipment = new Shipment("SHIP-3", 1.0);

        assertThatThrownBy(() -> service.fanOutBounded(shipment, List.of("acme"), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
